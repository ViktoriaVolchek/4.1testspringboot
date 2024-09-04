package com.itm.space.backendresources.service;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
public class UserServiceImplTest {

    @MockBean
    private Keycloak keycloakClient;

    @Autowired
    private UserServiceImpl userService;

    @MockBean
    private UserMapper userMapper;

    private final RealmResource realmResource = mock(RealmResource.class);
    private final UsersResource usersResource = mock(UsersResource.class);
    private final UserResource userResource = mock(UserResource.class);
    private final RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
    private final MappingsRepresentation mappingsRepresentation = mock(MappingsRepresentation.class);

    private final String REALM = "ITM";
    private UUID testUserId;

    @BeforeEach
    void setup() {

        when(keycloakClient.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(usersResource.create(any(UserRepresentation.class)))
                .thenReturn(Response.status(201).build());
        MockitoAnnotations.openMocks(this);
        testUserId = UUID.fromString("e5dfb182-d1a9-494c-a0ff-3965213a3143");
    }

    @Test
    public void testCreateUser_Success() {

        UserRequest userRequest = new UserRequest(
                "vika",
                "vika@gmail.com",
                "vika",
                "vika",
                "vika"
        );

        assertDoesNotThrow(() -> userService.createUser(userRequest));
        verify(keycloakClient.realm(REALM).users(), times(1)).create(any(UserRepresentation.class));
    }

    @Test
    public void testGetUserById_Success() {

        when(keycloakClient.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUserId.toString())).thenReturn(userResource);

        UserRepresentation mockUser = new UserRepresentation();
        mockUser.setId(testUserId.toString());
        mockUser.setEmail("vika@gmail.com");

        when(userResource.toRepresentation()).thenReturn(mockUser);

        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("ROLE_USER");

        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);
        when(mappingsRepresentation.getRealmMappings()).thenReturn(List.of(roleRepresentation));

        UserResponse mockResponse = new UserResponse(
                "vika",
                "vika",
                "vika@gmail.com",
                List.of("ROLE_USER"),
                List.of("group1", "group2"));

        when(userMapper.userRepresentationToUserResponse(any(UserRepresentation.class), anyList(), anyList()))
                .thenReturn(mockResponse);

        UserResponse response = userService.getUserById(testUserId);

        assertNotNull(response);
        assertEquals("vika@gmail.com", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertTrue(response.getGroups().contains("group1"));
        assertTrue(response.getGroups().contains("group2"));
    }

    @Test
    public void testGetUserById_Failure() {
        UUID userId = UUID.randomUUID();

        UserResource userResource = mock(UserResource.class);
        when(keycloakClient.realm(REALM).users().get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new RuntimeException("User not found"));

        BackendResourcesException exception = assertThrows(BackendResourcesException.class, () -> {
            userService.getUserById(userId);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        verify(userResource, times(1)).toRepresentation();
    }

    @Test
    public void testCreateUser() {

        UserRequest userRequest = new UserRequest(
                "vika",
                "vika@gmail.com",
                "vika",
                "vika",
                "vika"
        );

        when(keycloakClient.realm(REALM).users().create(any(UserRepresentation.class)))
                .thenThrow(new WebApplicationException(Response.status(500).build()));

        BackendResourcesException exception = assertThrows(BackendResourcesException.class, () -> {
            userService.createUser(userRequest);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }
}
