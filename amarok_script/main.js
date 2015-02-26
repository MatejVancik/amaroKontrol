/*
 *    Copyright (C) 2010 by Holger Reichert <mail@h0lger.de>    
 *    Copyright (C) 2009 by Johannes Wolter <jw@inutil.org>    
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

Importer.loadQtBinding("qt.core");
Importer.loadQtBinding("qt.network");
Importer.include("httpserver.js");
Importer.include("util.js");
Importer.include("amarokfunc.js");
Importer.include("conf.js");
Importer.include("const.js");

defaultHandler = function(path){
	response = new HandlerResponse();
	response.append("NACK");
	return response;
}

http = new HTTPServer();
http.setDefaultHandler(defaultHandler);
http.registerHandler("/getServerVersion/", getServerVersion);
http.registerHandler("/getState/", getState);
http.registerHandler("/getArtist/", getArtist);
http.registerHandler("/getTitle/", getTitle);
http.registerHandler("/getAlbum/", getAlbum);
http.registerHandler("/getLength/", getLength);
http.registerHandler("/getPosition/", getPosition);
http.registerHandler("/getCurrentCover/", getCurrentCover);
http.registerHandler("/getPlaylist/", getPlaylist)
http.registerHandler("/cmdPlaylistClear/", cmdPlaylistClear);
http.registerHandler("/cmdPrev/", cmdPrev);
http.registerHandler("/cmdNext/", cmdNext);
http.registerHandler("/cmdPlay/", cmdPlay);
http.registerHandler("/cmdPause/", cmdPause);
http.registerHandler("/cmdPlayPause/", cmdPlayPause);
http.registerHandler("/cmdStop/", cmdStop);
http.registerHandler("/cmdVolumeUp/", cmdVolumeUp);
http.registerHandler("/cmdVolumeDown/", cmdVolumeDown);
http.registerHandler("/cmdMute/", cmdMute);
http.registerHandler("/cmdPlayByIndex/", cmdPlayByIndex);
http.registerHandler("/cmdSetPosition/", cmdSetPosition);
http.registerHandler("/getCollectionAllArtists/", getCollectionAllArtists);
http.registerHandler("/getCollectionTracksByArtistId/", getCollectionTracksByArtistId);
http.registerHandler("/cmdCollectionPlayByTrackId/", cmdCollectionPlayByTrackId);
http.registerHandler("/cmdCollectionEnqueue/", cmdCollectionEnqueue);
http.registerHandler("/getCollectionSearchAll/", getCollectionSearchAll);

http.registerHandler("/getLyrics/", getLyrics);
http.registerHandler("/getCover/", getCover);
http.registerHandler("/getCurrentSong/", getCurrentSong);
http.registerHandler("/getCurrentSongJson/", getCurrentSongJson);
http.registerHandler("/getPlaylistJSON/", getPlaylistJSON);
http.registerHandler("/getCollectionAllArtistsJSON/", getCollectionAllArtistsJSON);
http.registerHandler("/getCollectionTracksByArtistIdJSON/", getCollectionTracksByArtistIdJSON);
http.registerHandler("/cmdRemoveByIndex/", cmdRemoveByIndex);
http.registerHandler("/getSongFile/", getSongFile);
http.registerHandler("/getCollectionSearchAllJSON/", getCollectionSearchAllJSON);