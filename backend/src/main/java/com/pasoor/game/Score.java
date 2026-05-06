package com.pasoor.game;

public record Score(
        int myScore,
        int opponentScore,
        int myClubCount,
        int opponentClubCount,
        int mySurPoints,
        int opponentSurPoints,
        int myCardPoints,
        int opponentCardPoints,
        int myAceCount,
        int opponentAceCount,
        int myJackCount,
        int opponentJackCount,
        int myTenOfDiamondsCount,
        int opponentTenOfDiamondsCount,
        int myTwoOfClubsCount,
        int opponentTwoOfClubsCount
) {
}
