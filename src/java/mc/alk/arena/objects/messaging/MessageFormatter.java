package mc.alk.arena.objects.messaging;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;

import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.messaging.MessageOptions.MessageOption;
import mc.alk.arena.objects.stats.ArenaStat;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.TimeUtil;


/**
 * @author alkarin
 *
 * at the moment I hate this class and how it works, but it works at the moment.
 * need to revisit this later
 */
public class MessageFormatter {
	final String[] searchList;
	final String[] replaceList;

	final Set<MessageOption> options;
	final Message msg;
	final HashMap<Integer,TeamNames> tns;
	final MatchParams params;
	final MessageSerializer messages;
	int commonIndex = 0, teamIndex = 0, curIndex = 0;
	
    public class TeamNames {
        public String longName, shortName, name;
    }
    
	public MessageFormatter( MessageSerializer impl, MatchParams mp, int nTeams, Message message, Set<MessageOption> ops ) {
		final int size = ops.size();
		searchList = new String[size];
		replaceList = new String[size];
		msg = message;
		options = ops;
		tns = new HashMap<>(nTeams);
		params = mp;
		messages = impl;
	}

	public void formatCommonOptions( Collection<ArenaTeam> teams, Integer seconds ) {
		int i = 0;
		ArenaTeam t1 = null, t2 = null;
		if ( teams != null ) {
			int j = 0;
			for ( ArenaTeam t : teams ) {
				if ( j == 0 )
					t1 = t;
				else if ( j == 1 )
					t2 = t;
				else
					break;
				j++;
			}
		}
		for ( MessageOption mop : options ) {
			if ( mop == null ) continue;
			switch( mop ) {
			case CMD: replaceList[i] = params.getCommand(); break;
			case PREFIX:
			case MATCHPREFIX:
			case EVENTPREFIX: replaceList[i] = params.getPrefix();
				break;
			case COMPNAME:
			case EVENTNAME:
			case MATCHNAME: replaceList[i] = params.getName();
				break;
			case SECONDS: replaceList[i] = seconds != null ? seconds.toString()
			                                               : null; 
			    break;
			case TIME: replaceList[i] = seconds != null ? TimeUtil.convertSecondsToString(seconds)
			                                            : null; 
			    break;
			case TEAM1:
				replaceList[i] = formatTeamName(messages.getNodeMessage("common.team"),t1);
				break;
			case TEAM2:
				replaceList[i] = formatTeamName(messages.getNodeMessage("common.team"),t2);
				break;
			case TEAMSHORT1:
				replaceList[i] = formatTeamName(messages.getNodeMessage("common.teamshort"),t1);
				break;
			case TEAMSHORT2:
				replaceList[i] = formatTeamName(messages.getNodeMessage("common.teamshort"),t2);
				break;
			case TEAMLONG1:
				replaceList[i] = formatTeamName(messages.getNodeMessage("common.teamlong"),t1);
				break;
			case TEAMLONG2:
				replaceList[i] = formatTeamName(messages.getNodeMessage("common.teamlong"),t2);
				break;
			case NTEAMS: replaceList[i] = teams != null ? teams.size() + "" 
			                                            : "0"; 
			    break;
			case PLAYERORTEAM: replaceList[i] = teams != null ? MessageUtil.getTeamsOrPlayers( params.getMaxTeamSize() ) 
			                                                  : "teams"; 
			    break;
			case PARTICIPANTS:
                if ( teams != null ) {
                    StringJoiner joiner = new StringJoiner( ", " );
                    
                    for ( ArenaTeam at: teams ) {
                        TeamNames tn = getTeamNames( at );
                        if ( tn == null )
                            joiner.add( at.getDisplayName() );
                        else if ( tn.longName != null )
                            joiner.add( tn.longName );
                        else if ( tn.shortName != null )
                            joiner.add( tn.shortName );
                        else if ( tn.name != null )
                            joiner.add( tn.name );
                        else 
                            joiner.add( at.getDisplayName() );
                    }
                    replaceList[i] = joiner.toString();
                }
                break;

			default:
				continue;
			}
			searchList[i++] = mop.getReplaceString();
		}
		commonIndex = i;
	}

	public void formatPlayerOptions(ArenaPlayer player){
		int i = commonIndex;
		for ( MessageOption mop : options ) {
			if ( mop == null ) continue;
			switch( mop ) {
			case PLAYERNAME: replaceList[i] = player.getDisplayName(); break;
			default:
				continue;
			}
			searchList[i++] = mop.getReplaceString();
		}
		teamIndex = i;
		curIndex = i;
	}

	public void formatTeamOptions(ArenaTeam team, boolean isWinner){

		int i = commonIndex;
		TeamNames tn = getTeamNames(team);
		for ( MessageOption mop : options ) {
			if ( mop == null ) continue;
			
			switch( mop ) {
			case WINNER: replaceList[i] = isWinner ? team.getDisplayName() : ""; break;
			case LOSER: replaceList[i] = !isWinner ? team.getDisplayName() : ""; break;
			case NAME: replaceList[i] = team.getDisplayName(); break;
			case TEAM: replaceList[i] = tn.name; break;
			case TEAMSHORT: replaceList[i] = tn.shortName; break;
			case TEAMLONG: replaceList[i] = tn.longName; break;
			case WINS: replaceList[i] = team.getStat().getWins() + ""; break;
			case LOSSES: replaceList[i] = team.getStat().getLosses() + ""; break;
			case RANKING: replaceList[i] = team.getStat().getRanking() + ""; break;
			case RATING: replaceList[i] = team.getStat().getRating() + ""; break;
			default:
				continue;
			}
			searchList[i++] = mop.getReplaceString();
		}
		teamIndex = i;
		curIndex = i;
	}

	public void formatTwoTeamsOptions(ArenaTeam t, Collection<ArenaTeam> teams){
		ArenaTeam oteam;
		ArenaStat st1 = t.getStat();
		int i = teamIndex;
		for ( MessageOption mop : options ) {
			if ( mop == null ) continue;
			
			String repl = null;
			switch( mop ) {
    			case OTHERTEAM:
    				oteam = getOtherTeam( t, teams );
    				if ( oteam != null )
    					repl = oteam.getDisplayName();
    				break;
    				
    			case WINSAGAINST:
					oteam = getOtherTeam( t, teams );
					if ( oteam != null ) {
						ArenaStat st2 = oteam.getStat();
						repl = st1.getWinsVersus(st2) + "";
					} 
					else repl = "0";
    				break;
    				
    			case LOSSESAGAINST:
					oteam = getOtherTeam( t, teams );
					if ( oteam != null ) {
						ArenaStat st2 = oteam.getStat();
						repl = st1.getLossesVersus(st2) + "";
					} 
					else repl = "0";
    				break;
    				
    			default:
    				continue;
			}
			searchList[i] = mop.getReplaceString();
			replaceList[i++] = repl;
		}
		curIndex = i;
	}

	public void formatTeams(Collection<ArenaTeam> teams){
		if ( options.contains( MessageOption.TEAMS ) ) {
		    StringJoiner joiner = new StringJoiner( ", " );
		    
			for ( ArenaTeam team : teams ) {
				joiner.add( team.getDisplayName() );
			}
			replaceList[curIndex] = joiner.toString();
			searchList[curIndex] = MessageOption.TEAMS.getReplaceString();
			curIndex++;
		}
	}

	public void formatWinnerOptions( ArenaTeam team, boolean isWinner ) {
		int i = curIndex;
		TeamNames tn = getTeamNames( team );
		for ( MessageOption mop : options ) {
			if ( mop == null ) continue;
			
			switch( mop ) {
    			case WINNER:
    				if ( !isWinner ) continue;
    				replaceList[i] = tn.name;
    				break;
    				
    			case WINNERSHORT:
    				if ( !isWinner ) continue;
    				replaceList[i] = tn.shortName;
    				break;
    				
    			case WINNERLONG:
    				if ( !isWinner ) continue;
    				replaceList[i] = tn.longName;
    				break;
    				
    			case LOSER:
    				if ( isWinner ) continue;
    				replaceList[i] = tn.name;
    				break;
    				
    			case LOSERSHORT:
    				if ( isWinner ) continue;
    				replaceList[i] = tn.shortName;
    				break;
    				
    			case LOSERLONG:
    				if ( isWinner ) continue;
    				replaceList[i] = tn.longName;
    				break;
    				
    			case LIFELEFT:
    				if ( !isWinner ) continue;
    				
    				StringJoiner joiner = new StringJoiner( ", " );

					for ( ArenaPlayer ap : team.getLivingPlayers() ) {
						joiner.add( String.join( "", "&6", ap.getDisplayName(), 
						                             "&e(&4", String.valueOf( ap.getHealth() ), "&e)" ) );
					}
					for ( ArenaPlayer ap : team.getDeadPlayers() ) {
						joiner.add( String.join( "", "&6", ap.getDisplayName(), "&e(&8Dead&e)" ) );
					}
					replaceList[i] = joiner.toString();
					break;
				
    			default:
    				continue;
			}
			searchList[i++] = mop.getReplaceString();
		}
		curIndex = i;
	}

	private TeamNames getTeamNames( ArenaTeam t ) {
		if ( tns.containsKey( t.getId() ) )
			return tns.get( t.getId() );
		
		TeamNames tn = new TeamNames();
		formatTeamNames( options, t, tn );
		tns.put( t.getId(), tn );
		return tn;
	}

	private ArenaTeam getOtherTeam(ArenaTeam t, Collection<ArenaTeam> teams) {
		for ( ArenaTeam oteam : teams ) {
			if ( oteam.getId() != t.getId() ) 
				return oteam;
		}
		return null;
	}

    public void formatTeamNames( Set<MessageOption> opts, ArenaTeam team, TeamNames tn ) {
        if    ( opts.contains( MessageOption.TEAM ) 
                || opts.contains( MessageOption.WINNER ) 
                || opts.contains ( MessageOption.LOSER ) ) {
            tn.name = formatTeamName( messages.getNodeMessage( "common.team" ), team );
        }
        if    ( opts.contains( MessageOption.TEAMSHORT ) 
                || opts.contains( MessageOption.WINNERSHORT ) 
                || opts.contains( MessageOption.LOSERSHORT ) ) {
            tn.shortName = formatTeamName( messages.getNodeMessage( "common.teamshort" ), team );
        }
        if    ( opts.contains( MessageOption.TEAMLONG ) 
                || opts.contains( MessageOption.WINNERLONG ) 
                || opts.contains( MessageOption.LOSERLONG ) ) {
            tn.longName = formatTeamName( messages.getNodeMessage( "common.teamlong" ), team );
        }
    }

	private String formatTeamName( Message message, ArenaTeam t ) {
		if ( t == null ) return null;
		
		Set<MessageOption> opts = message.getOptions();
		String[] searches = new String[opts.size()];
		String[] replaces = new String[opts.size()];

		int i = 0;

		for ( MessageOption mop : opts ) {
			if ( mop == null ) continue;

			String repl;
			switch( mop ) {
			case NAME: repl = t.getDisplayName(); break;
			case WINS: repl = t.getStat().getWins() + ""; break;
			case LOSSES: repl = t.getStat().getLosses() + "" ; break;
			case RANKING: repl = t.getStat().getRanking() + ""; break;
			case RATING: repl = t.getStat().getRating() + ""; break;
			default:
				continue;
			}
			searches[i] = mop.getReplaceString();
			replaces[i++] = repl;
		}
		return StringUtils.replaceEachRepeatedly( message.getMessage(), searches, replaces );
	}
	
    public String getFormattedMessage( Message message ) {
        return StringUtils.replaceEachRepeatedly( message.getMessage(), searchList, replaceList );
    }

	public void printMap(){
		Log.info( "!!!!!!!!!!!!!! " + commonIndex + "   " + teamIndex );
		for ( int i = 0; i < searchList.length; i++ ) {
			Log.info( i +" : " + replaceList[i] + "  ^^^ " + searchList[i] );
		}
	}
}
