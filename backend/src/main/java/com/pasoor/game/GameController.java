package com.pasoor.game;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
@RestController
@RequestMapping("/api/game")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/new")
    public GameState newGame() {
        return gameService.newGame();
    }

    @GetMapping
    public GameState getGame() {
        return gameService.getGame();
    }

    @PostMapping("/deal")
    public GameState deal() {
        return gameService.deal();
    }

    @PostMapping("/play")
    public GameState playCard(@RequestBody PlayCardRequest request) {
        return gameService.playCard(request);
    }

    @PostMapping("/collect")
    public GameState collectCards(@RequestBody CollectCardsRequest request) {
        return gameService.collectCards(request);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<String> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
