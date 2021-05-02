package ch.uzh.ifi.hase.soprafs21.controller;

import ch.uzh.ifi.hase.soprafs21.entity.*;
import ch.uzh.ifi.hase.soprafs21.rest.dto.*;
import ch.uzh.ifi.hase.soprafs21.rest.mapper.ChatDTOMapper;
import ch.uzh.ifi.hase.soprafs21.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs21.rest.mapper.LobbyDTOMapper;
import ch.uzh.ifi.hase.soprafs21.rest.mapper.MessageDTOMapper;
import ch.uzh.ifi.hase.soprafs21.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class LobbyController {

    private final LobbyService lobbyService;

    LobbyController(LobbyService lobbyService) {
            this.lobbyService = lobbyService;
        }

    // Get mapping to /lobbies to fetch all lobbies to the frontend

    @GetMapping("/lobbies")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LobbyGetDTO> getAllLobbies() {
       // fetch all lobbies in the internal representation
       List<Lobby> lobbies = lobbyService.getLobbies();
       List<LobbyGetDTO> lobbyGetDTOs = new ArrayList<>();

       // convert each lobby to the API representation
       for (Lobby lobby : lobbies) {
            lobbyGetDTOs.add(LobbyDTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby));
            }
            return lobbyGetDTOs;
        }

    @PostMapping("/lobbies/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyGetDTO createLobby(@PathVariable Long userId, @RequestBody LobbyPostDTO lobbyPostDTO) {
        // convert API lobby to internal representation
        Lobby lobbyInput = LobbyDTOMapper.INSTANCE.convertLobbyPostDTOtoEntity(lobbyPostDTO);

        // create lobby
        Lobby createdLobby = lobbyService.createLobby(lobbyInput, userId);

        // convert internal representation of lobby back to API
        return LobbyDTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
    }

    // Put mapping to  /lobbies/{lobbyId} to update the lobby setting in the repository

    @PutMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public  void editCurrentLobby(@PathVariable Long lobbyId, @RequestBody LobbyPostDTO lobbyEditDTO) {
        // convert API lobby to internal representation
        Lobby lobbyInput = LobbyDTOMapper.INSTANCE.convertLobbyPostDTOtoEntity(lobbyEditDTO);
        lobbyService.update_lobby(lobbyId, lobbyInput);
    }

    // Get mapping to /lobbies/{lobbyId} to get the lobby by its Id

    @GetMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO getLobbyByID(@PathVariable Long lobbyId) {
        Lobby lobby = lobbyService.getLobby(lobbyId);
        // convert internal representation of lobby back to API
        LobbyGetDTO lobbyGetDTO = LobbyDTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
        return lobbyGetDTO;
    }

    // Put mapping to add a member to the lobby & enter it

    @PutMapping("/lobbies/{lobbyId}/joiners")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO addMember(@PathVariable Long lobbyId, @RequestBody LobbyPostDTO lobbyEnterDTO) {
        Lobby lobbyInput = LobbyDTOMapper.INSTANCE.convertLobbyPostDTOtoEntity(lobbyEnterDTO);
        lobbyService.add_lobby_members(lobbyId, lobbyInput);
        Lobby lobby = lobbyService.getLobby(lobbyId);
        LobbyGetDTO lobbyGetDTO = LobbyDTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
        return lobbyGetDTO;
    }

    // Put mapping to remove a member from the lobby

    @PutMapping("/lobbies/{lobbyId}/leavers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void removeMember(@PathVariable Long lobbyId, @RequestBody UserPostDTO userPostDTO) {
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        lobbyService.remove_lobby_members(lobbyId, userInput.getUsername());
    }

/*
    //API Call for getting the chat in the lobby
    @GetMapping("/lobbies/{lobbyId}/messages")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ChatGetDTO getMessages (@PathVariable Long lobbyId, @RequestBody ChatPostDTO chatPostDTO) {
        LocalDateTime timeStamp = ChatDTOMapper.INSTANCE.convertChatPostDTOtoEntity(chatPostDTO);
        Lobby lobby = lobbyService.getLobby(lobbyId);
        Chat chat = lobby.getLobbyChat(timeStamp);
        return ChatDTOMapper.INSTANCE.convertEntityToChatGetDTO(chat);
    }

 */
/*
    //API Call for posting a message in the lobby
    @PutMapping("/lobbies/{lobbyId}/messages")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void Message (@PathVariable Long lobbyId, @RequestBody MessagePostDTO messagePostDTO) {
        Message message = MessageDTOMapper.INSTANCE.convertMessagePostDTOtoEntity(messagePostDTO);
        Lobby lobby = lobbyService.getLobby(lobbyId);
        lobby.setLobbyChat(message);
        return;
    }

 */
}
