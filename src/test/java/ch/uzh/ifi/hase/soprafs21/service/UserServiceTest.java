package ch.uzh.ifi.hase.soprafs21.service;

import ch.uzh.ifi.hase.soprafs21.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // given
        testUser = new User();
        testUser.setId(1L);
        testUser.setPassword("testPassword");
        testUser.setUsername("testUsername");


        // when -> any object is being save in the userRepository -> return the dummy testUser
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(java.util.Optional.ofNullable(testUser));
    }

    @Test
    public void createUser_validInputs_success() {
        // when -> any object is being save in the userRepository -> return the dummy testUser
        User createdUser = userService.createUser(testUser);

        // then
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

        assertEquals(testUser.getId(), createdUser.getId());
        assertEquals(testUser.getPassword(), createdUser.getPassword());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotNull(createdUser.getToken());
        assertEquals(UserStatus.ONLINE, createdUser.getStatus());


    }


    @Test
    public void createUser_duplicateName_throwsException() {
        // given -> a first user has already been created
        userService.createUser(testUser);

        // when -> setup additional mocks for UserRepository
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

        // then -> attempt to create second user with same user -> check that an error is thrown
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void getUsers_byID(){
        User createdUser = userService.createUser(testUser);
        User foundUser = userService.getUserById(testUser.getId());

        Mockito.verify(userRepository, Mockito.times(1)).findById(Mockito.any());

        assertEquals(foundUser.getId(), createdUser.getId());
        assertEquals(foundUser.getUsername(), createdUser.getUsername());
        assertNotNull(createdUser.getToken());

    }
    @Test
    public void getUsers_byID_unsuccessfull(){
        User createdUser = userService.createUser(testUser);
        User foundUser = userService.getUserById(1000L);

        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> userService.getUserById(1L));

    }


    @Test
    public  void logoutUser_setUserStatus_toOFFLINE_success(){
        // given the user is created automatically ONLINE
        User createdUser = userService.createUser(testUser);

        assertEquals(UserStatus.ONLINE, createdUser.getStatus());
        // then
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
        userService.logout(testUser.getId());

        assertEquals(UserStatus.OFFLINE, createdUser.getStatus());

    }
    @Test
    public void  loginUser_sucessfull(){
        // given the a user is created
        User createdUser = userService.createUser(testUser);
        createdUser.setStatus(UserStatus.OFFLINE);
        assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
        // when --> Setup additional mocks
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
        User logedinUser = userService.loginRequest(createdUser);

        assertEquals(testUser.getPassword(), logedinUser.getPassword());
        assertEquals(testUser.getUsername(), logedinUser.getUsername());
        assertNotNull(logedinUser.getToken());
        assertEquals(UserStatus.ONLINE, logedinUser.getStatus());



    }

    @Test
    public void loginRequest_Usernotregistered_throwError(){

        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> userService.loginRequest(testUser));
    }
    @Test
    public void loginRequest_wrongPassword_throwError(){
        User createdUser = userService.createUser(testUser);

        User fake_User = new User();
        fake_User.setId(1L);
        fake_User.setPassword("abc");
        fake_User.setUsername("testUsername");
        fake_User.setStatus(UserStatus.OFFLINE);

        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(fake_User);

        assertThrows(ResponseStatusException.class, () -> userService.loginRequest(createdUser));
        assertEquals(UserStatus.OFFLINE, fake_User.getStatus());
    }
    @Test
    public void updateUser_successfull(){
        User userchanges = new User();
        userchanges.setUsername("Updated");
        userchanges.setBirthDate("01.01.2000");

        assertNotEquals(testUser.getUsername(), userchanges.getUsername());
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
        userService.updateUser(testUser.getId(), userchanges);

        assertEquals(testUser.getUsername(), userchanges.getUsername());
        assertEquals(testUser.getBirthDate(), userchanges.getBirthDate());


    }

    @Test
    public void updateUser_successfull_withNullvalues(){
        User userchanges = new User();
        userchanges.setUsername(null);
        userchanges.setBirthDate(null);

        assertNotEquals(testUser.getUsername(), userchanges.getUsername());
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
        userService.updateUser(testUser.getId(), userchanges);

        assertEquals(testUser.getUsername(), testUser.getUsername());
        assertEquals(testUser.getBirthDate(), testUser.getBirthDate());


    }


    @Test
    public void updateUser_unsuccessfull(){
        User existinguser = new User();
        existinguser.setUsername("Updated");
        String unchanged= testUser.getUsername();

        User userchanges = new User();
        userchanges.setUsername("Updated");
        userchanges.setBirthDate("01.01.2000");

        assertNotEquals(testUser.getUsername(), userchanges.getUsername());
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(existinguser);

        assertThrows(ResponseStatusException.class, () -> userService.updateUser(testUser.getId(),userchanges));
        assertEquals(testUser.getUsername(),unchanged);

}
@Test
    public void addUserasFriend_success(){
        User newFriend = new User();
        newFriend.setUsername("Friend1");
        newFriend.setId(2L);

        userService.addUserToFriendsList(testUser.getId(), newFriend);
        assertTrue(testUser.getFriendsList().contains(newFriend.getUsername()));


}
@Test
    public void add_user_again_fail(){
    testUser.setFriendsList("Friend1");
    User newFriend = new User();
    newFriend.setUsername("Friend1");
    newFriend.setId(2L);

    assertThrows(ResponseStatusException.class, () -> userService.addUserToFriendsList(testUser.getId(),newFriend));


}
@Test
    public void removeUser_fromFL_success(){
    User delete = new User();
    delete.setUsername("delete");
    testUser.setFriendsList("delete");
    testUser.setFriendsList("Friend1");
    userService.removeUserFromFriendsList(testUser.getId(),delete);
    assertTrue(testUser.getFriendsList().contains("Friend1"));
    assertFalse(testUser.getFriendsList().contains(delete.getUsername()));
}
@Test
    public void Remove_random_persone_failed(){
    User delete = new User();
    delete.setUsername("delete");

    assertThrows(ResponseStatusException.class, () -> userService.removeUserFromFriendsList(testUser.getId(),delete));

    }
}
