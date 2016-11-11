package mc.alk.arena.objects.options;

import lombok.AllArgsConstructor;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.victoryconditions.VictoryType;
import mc.alk.arena.serializers.ConfigSerializer;
import mc.alk.arena.util.MinMax;

@AllArgsConstructor
public enum AlterParamOption {
    NLIVES ("nLives", true, false),
    TEAMSIZE("teamSize",true, false),
    NTEAMS("nTeams",true, false),
    PREFIX("prefix",true, false),
    PRESTARTTIME("secondsTillMatch",true, false),
    FORCESTARTTIME("forceStartTime",true, false),
    MATCHTIME("matchTime",true, false),
    GAMETYPE("gameType",true, false),
    VICTORYTIME("secondsToLoot",true, false),
    VICTORYCONDITION("victoryCondition",true, false),
    NCUMONCURRENTCOMPETITIONS("numConcurrentCompetitions", true, false),
    COMMAND("command",true, false),
    SIGNDISPLAYNAME("signDisplayName",true, false),
    DISPLAYNAME("displayName",true, false),
    DATABASE("db",true, false),
    RATED("rated",true, false),
    USETRACKERMESSAGES("useTrackerMessages",true, false),
    GIVEITEMS("giveItems",true, false),
    NEEDITEMS("needItems",true, false),
    TAKEITEMS("takeItems",true, false),
    ALLOWEDTEAMSIZEDIFFERENCE("allowedTeamSizeDifference",true,false),
    CLOSEWAITROOMWHILERUNNING("closeWaitroomWhileRunning", true,false),
    CANCELIFNOTENOUGHPLAYERS("cancelIfNotEnoughPlayers", true,false);

    final String name;
    final boolean needsValue; 
    final boolean needsPlayer; 

    public boolean hasValue() { return needsValue; }
    public boolean needsPlayer() { return needsPlayer; }
    @Override
    public String toString(){ return name; }

    public static AlterParamOption fromString(String str){
        str = str.toUpperCase();
        try {
            return valueOf(str);
        } catch (IllegalArgumentException e){
            
            if (str.equalsIgnoreCase("secondsTillMatch") || str.equalsIgnoreCase("secondsUntilMatch"))
                return PRESTARTTIME;
            
            if (str.equalsIgnoreCase("gameTime")) return MATCHTIME;
            if (str.equalsIgnoreCase("numTeams")) return NTEAMS;
            if (str.equalsIgnoreCase("secondsToLoot")) return VICTORYTIME;
            if (str.equalsIgnoreCase("victoryTime")) return VICTORYTIME;
            if (str.equalsIgnoreCase("items")) return GIVEITEMS;
            if (str.equalsIgnoreCase("db") || str.equalsIgnoreCase("dbTableName")) return DATABASE;
            if (str.equalsIgnoreCase("waitroomClosedWhileRunning")) return CLOSEWAITROOMWHILERUNNING;
            if (str.equalsIgnoreCase("nConcurrentCompetitions")) return NCUMONCURRENTCOMPETITIONS;
            return null;
        }
    }

    public static Object getValue(AlterParamOption go, String value) {
        switch (go){
            case TEAMSIZE:
            case NTEAMS:
                return MinMax.valueOf(value);
            case MATCHTIME:
                return ConfigSerializer.toPositiveSize(value, 30);
            case VICTORYTIME:
            case PRESTARTTIME:
            case FORCESTARTTIME:
                return ConfigSerializer.toNonNegativeSize(value, 1);
            case NLIVES:
                return ConfigSerializer.toPositiveSize(value, ArenaSize.MAX);
            case NCUMONCURRENTCOMPETITIONS:
                return ConfigSerializer.toPositiveSize(value, ArenaSize.MAX);
            case ALLOWEDTEAMSIZEDIFFERENCE:
                return ConfigSerializer.toNonNegativeSize(value, 1);
            case PREFIX:
            case COMMAND:
            case DATABASE:
            case DISPLAYNAME:
            case SIGNDISPLAYNAME:
                return value;
            case VICTORYCONDITION:
                return VictoryType.fromString(value);
            case GAMETYPE:
                return ArenaType.getType(value);
 
            case CANCELIFNOTENOUGHPLAYERS:
            case CLOSEWAITROOMWHILERUNNING:
            case RATED:
            case USETRACKERMESSAGES:
                return Boolean.valueOf(value);
            default:
        }
        return null;
    }

}
