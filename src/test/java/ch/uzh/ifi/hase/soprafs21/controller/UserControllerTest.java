package ch.uzh.ifi.hase.soprafs21.controller;

import ch.uzh.ifi.hase.soprafs21.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.exceptions.GlobalExceptionAdvice;
import ch.uzh.ifi.hase.soprafs21.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs21.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs21.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // test the getUsers correspondence
    @Test
    public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
        // given
        User user = new User();
        user.setPassword("Firstname Lastname");
        user.setUsername("firstname@lastname");
        user.setStatus(UserStatus.OFFLINE);

        List<User> allUsers = Collections.singletonList(user);

        // this mocks the UserService -> we define above what the userService should return when getUsers() is called
        given(userService.getUsers()).willReturn(allUsers);

        // when
        MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].password", is(user.getPassword())))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
    }

    // test the createUser correspondence
    // /users POST: Code 201 --> Correct Input
    @Test
    public void createUser_validInput_userCreated() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setPassword("Test Password");
        user.setUsername("testUsername");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setPassword("Test Password");
        userPostDTO.setUsername("testUsername");

        given(userService.createUser(Mockito.any())).willReturn(user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.password", is(user.getPassword())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
    }

    // /users POST: Code 409 --> Wrong Input
    @Test
    public void createUser_invalidInput_existingUserCreated() throws Exception {
        // given
        User existing_user = new User();
        existing_user.setId(1L);
        existing_user.setPassword("Test Password");
        existing_user.setUsername("testUsername");
        existing_user.setToken("1");
        existing_user.setStatus(UserStatus.OFFLINE);

        UserPostDTO userPostDTO1 = new UserPostDTO();
        userPostDTO1.setPassword("Test Password");
        userPostDTO1.setUsername("testUsername");

        given(userService.createUser(Mockito.any())).willReturn(existing_user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO1));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated());

        System.out.println(userService.getUsers());

        User created_user = new User();
        created_user.setId(1L);
        created_user.setPassword("Test Password2");
        created_user.setUsername("testUsername");
        created_user.setToken("2");
        created_user.setStatus(UserStatus.OFFLINE);

        UserPostDTO userPostDTO2 = new UserPostDTO();
        userPostDTO2.setPassword("Test Password");
        userPostDTO2.setUsername("testUsername");

        given(userService.createUser(Mockito.any())).willThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        MockHttpServletRequestBuilder postRequest2 = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO2));

        mockMvc.perform(postRequest2)
                .andExpect(status().isConflict());

    }


    // test the loginUser correspondence
    // /login PUT: Code 204 --> correct input
    @Test
    public void loginUser_validInput_userLoggedIn() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setPassword("Test Password");
        user.setUsername("testUsername");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setPassword("Test Password");
        userPostDTO.setUsername("testUsername");

        given(userService.login_request(Mockito.any())).willReturn(user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder putRequest = put("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.password", is(user.getPassword())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
    }

    // /login PUT: Code 404 --> wrong input
    @Test
    public void loginUser_invalidInput() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setPassword("Test Password");
        userPostDTO.setUsername("testUsername");

        given(userService.login_request(Mockito.any())).willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder putRequest = put("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }


    // test the getUser correspondence
    // /users/{userId} GET: Code 200 --> correct input

    @Test
    public void getUser_validInput() throws Exception {
        // given
        User user = new User();
        user.setPassword("Firstname Lastname");
        user.setUsername("firstname@lastname");
        user.setCreation_date("08-03-2020");
        user.setBirth_date("01.01.1980");
        user.setStatus(UserStatus.OFFLINE);
        user.setId(1L);

        given(userService.getUser(1L)).willReturn(user);

        MockHttpServletRequestBuilder getRequest = get("/users/1").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$.password", is(user.getPassword())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.creation_date", is(user.getCreation_date())))
                .andExpect(jsonPath("$.birth_date", is(user.getBirth_date())))
                .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
    }

    // /users/{userId} GET: Code 404 --> wrong input

    @Test
    public void getUser_notExistingUser() throws Exception {

        given(userService.getUser(Mockito.anyLong())).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        MockHttpServletRequestBuilder getRequest = get("/users/99")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest).andExpect(status().isNotFound());
    }

    // test the updateUser correspondence

    // /users/{userId} PUT: Code 204 --> correct input

    @Test
    public void updateUser_validInput() throws Exception {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("username");
        userPostDTO.setBirth_date("01.01.1980");
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        doNothing().when(userService).update_user(1L, userInput);

        MockHttpServletRequestBuilder putRequest = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(putRequest).andExpect(status().isNoContent()); }


    /**
     * Helper Method to convert userPostDTO into a JSON string such that the input can be processed
     * Input will look like this: {"name": "Test User", "userpassword": "testUsername"}
     * @param object
     * @return string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("The request body could not be created.%s", e.toString()));
        }
    }
}