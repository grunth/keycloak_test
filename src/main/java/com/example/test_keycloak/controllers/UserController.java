package com.example.test_keycloak.controllers;

import com.example.test_keycloak.dto.UserDTO;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.keycloak.authorization.client.Configuration;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RequestMapping(value = "/users")
@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private String authServerUrl = "http://localhost:8080/auth";
    private String realm = "SpringBootKeycloak";
    private String clientId = "user1";
    private String role = "user";
    private String clientSecret = "45fa9223-9ac6-4242-99c9-1ff9a33d5253";

    @GetMapping(value = "/all")
    public String getAllusers() {
        System.out.println("петька");

//        ResteasyProviderFactory instance=ResteasyProviderFactory.getInstance();
//        RegisterBuiltin.register(instance);
//        instance.registerProvider(ResteasyJackson2Provider.class);

        Keycloak keycloak = KeycloakBuilder
                .builder()
                .serverUrl(authServerUrl)
                .grantType(OAuth2Constants.PASSWORD)
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password("123")
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build())
                .build();
        // Get realm
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersRessource = realmResource.users();
        UserResource userResource = usersRessource.get("45fa9223-9ac6-4242-99c9-1ff9a33d5253");
        return userResource.toString();
    }

    @GetMapping(value = "/all2")
    public String getAllusers2() {
        return "Привет2";
    }

    @PostMapping(path = "/create")
    public ResponseEntity<?> createUser(@RequestBody UserDTO userDTO) {

        Keycloak keycloak = KeycloakBuilder.builder().serverUrl(authServerUrl)
                .grantType(OAuth2Constants.PASSWORD).realm("master").clientId("57382466-c5a4-4e4f-8862-f655ab1a2763")
                .username("admin").password("Takamba24")
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build()).build();

        keycloak.tokenManager().getAccessToken();


        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstname());
        user.setLastName(userDTO.getLastname());
        user.setEmail(userDTO.getEmail());

        // Get realm
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersRessource = realmResource.users();

        Response response = usersRessource.create(user);

        userDTO.setStatusCode(response.getStatus());
        userDTO.setStatus(response.getStatusInfo().toString());

        if (response.getStatus() == 201) {

            String userId = CreatedResponseUtil.getCreatedId(response);

            log.info("Created userId {}", userId);


            // create password credential
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(userDTO.getPassword());

            UserResource userResource = usersRessource.get(userId);

            // Set password credential
            userResource.resetPassword(passwordCred);

            // Get realm role student
            RoleRepresentation realmRoleUser = realmResource.roles().get(role).toRepresentation();

            // Assign realm role student to user
            userResource.roles().realmLevel().add(Arrays.asList(realmRoleUser));
        }
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping(path = "/signin")
    public ResponseEntity<?> signin(@RequestBody UserDTO userDTO) {

        Map<String, Object> clientCredentials = new HashMap<>();
        clientCredentials.put("secret", clientSecret);
        clientCredentials.put("grant_type", "password");

        Configuration configuration =
                new Configuration(authServerUrl, realm, clientId, clientCredentials, null);
        AuthzClient authzClient = AuthzClient.create(configuration);

        AccessTokenResponse response =
                authzClient.obtainAccessToken(userDTO.getEmail(), userDTO.getPassword());

        return ResponseEntity.ok(response);
    }


    @GetMapping(value = "/unprotected-data")
    public String getName() {
        return "Hello, this api is not protected.";
    }


    @GetMapping(value = "/protected-data")
    public String getEmail() {
        return "Hello, this api is protected.";
    }

}