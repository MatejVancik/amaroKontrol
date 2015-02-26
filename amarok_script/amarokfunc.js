/*
 *    Copyright (C) 2013 by Matej Vancik <depeha@gmail.com>
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
Importer.loadQtBinding("qt.gui");
Importer.include("httpserver.js");
Importer.include("util.js");
Importer.include("const.js");

/* General commands */

getServerVersion = function(path){
	response = new HandlerResponse();
	sv = SERVERVERSION
	response.append(sv.toString());
	return response;
}


/* Player commands */

getState = function(path){
	response = new HandlerResponse();
	response.append(Amarok.Engine.engineState().toString());
	return response;
}

getArtist = function(path){
	response = new HandlerResponse();
	response.append(shorten(Amarok.Engine.currentTrack().artist, SHORTEN1));
	return response;
}

getTitle = function(path){
	response = new HandlerResponse();
	response.append(shorten(Amarok.Engine.currentTrack().title, SHORTEN1));
	return response;
}

getAlbum = function(path){
	response = new HandlerResponse();
	response.append(shorten(Amarok.Engine.currentTrack().album, SHORTEN1));
	return response;
}

getCurrentSong = function(path){
  response = new HandlerResponse();
  
  artist = Amarok.Engine.currentTrack().artist;
  album = Amarok.Engine.currentTrack().album;
  title = Amarok.Engine.currentTrack().title;  
  id = 0;
  
  for(trackidx=0; trackidx<Amarok.Playlist.totalTrackCount(); trackidx=trackidx+1){
    t = Amarok.Playlist.trackAt(trackidx);
    if(t.artist == artist && t.album == album && t.title == title){
      id = trackidx.toString();
      break;
    }
  }
  result = id+"\n"+artist+"\n"+album+"\n"+title;
  
  response.append(result.toString());
  return response;
}

getCurrentSongJson = function(path){
	response = new HandlerResponse(true);
	
	
	artist = jsonEscape(Amarok.Engine.currentTrack().artist);
	album = jsonEscape(Amarok.Engine.currentTrack().album);
	title = jsonEscape(Amarok.Engine.currentTrack().title); 
	id = 0;
	
	for(trackidx=0; trackidx<Amarok.Playlist.totalTrackCount(); trackidx=trackidx+1){
		t = Amarok.Playlist.trackAt(trackidx);
		if(t.artist == artist && t.album == album && t.title == title){
			id = trackidx.toString();
			break;
		}
	}
	
	
	var currentTrack = '"id":' + id + ',';
	currentTrack += '"title":"' + title + '",';
	currentTrack += '"artist":"' + artist + '",';
	currentTrack += '"album":"' + album + '",';
	currentTrack += '"length":' + Amarok.Engine.currentTrack().length + ',';
	currentTrack += '"position":' + Amarok.Engine.trackPositionMs() + ',';
	currentTrack += '"status":' + Amarok.Engine.engineState().toString();
	
	response.append('{"status":"OK","currentTrack":{'+currentTrack+'}}');
	return response;
}

getLength = function(path){
	response = new HandlerResponse();
	length = Amarok.Engine.currentTrack().length;
	if (Amarok.Info.version() == "2.2.0") length = length * 1000;
	response.append(length.toString());
	return response;
}

getPosition = function(path){
	response = new HandlerResponse();
	response.append(Amarok.Engine.trackPositionMs().toString());
	return response;
}

getCurrentCover = function(path){
    dimen = parseInt(path.substring(path.lastIndexOf("/")+1));
    if(isNaN(dimen)) dimen = 512;
    response = new HandlerResponse();
    response.setMimeType("image/png");
    engineState = Amarok.Engine.engineState();
    if(engineState == ENGINE_STATE_PAUSE || engineState == ENGINE_STATE_PLAY){
        response.append(pixmapToPNG(Amarok.Engine.currentTrack().imagePixmap(), dimen));
    }
    return response;
}

getSongFile = function(path){
    position = parseInt(path.substring(path.lastIndexOf("/")+1));
    fileNames = Amarok.Playlist.trackAt(position).path.toString();
    response = new HandlerResponse();
    name = new QFileInfo(fileNames);
    file = new QFile(name.canonicalFilePath());
    if(file.open(QIODevice.ReadOnly)){
	    response.setMimeType(name.completeSuffix().toString());
    }
    response.append(file.readAll());
    file.close();
    return response;
}

getPlaylist = function(path){
    response = new HandlerResponse();
    tracks = "";
    for(trackidx=0; trackidx<Amarok.Playlist.totalTrackCount(); trackidx=trackidx+1){
        t = Amarok.Playlist.trackAt(trackidx);
	if(t.artist=="") tmpArtist = "---";
	else tmpArtist = jsonEscape(t.artist);
	
	if(t.title=="") tmpTitle = "---";
	else tmpTitle = jsonEscape(t.title);
	
	if(t.album=="") tmpAlbum = " ";
	else tmpAlbum = jsonEscape(t.album);
	
	tracks += trackidx.toString() + '\r';
	tracks += shorten(tmpArtist, SHORTEN2) + '\r';
	tracks += shorten(tmpTitle, SHORTEN2) + '\r';
	tracks += shorten(tmpAlbum, SHORTEN2) + '\r';
	tracks += '\n';	
    }
    response.append(tracks);
    return response;
}

getPlaylistJSON = function(path){
	response = new HandlerResponse(true);
	tracks = "";
	var totalTrackCount = Amarok.Playlist.totalTrackCount();
	for(trackidx=0; trackidx<totalTrackCount; trackidx++){
		t = Amarok.Playlist.trackAt(trackidx);
		if(t.artist=="") tmpArtist = "---";
		else tmpArtist = jsonEscape(t.artist);
		
		if(t.title=="") tmpTitle = "---";
		else tmpTitle = jsonEscape(t.title);
		
		if(t.album=="") tmpAlbum = " ";
		else tmpAlbum = jsonEscape(t.album);
		
		var track = '"id":'+ trackidx + ',';
		track += '"title":"'+ tmpTitle + '",';
		track += '"artist":"'+ tmpArtist + '",';
		track += '"album":"'+ tmpAlbum+ '"';
		tracks += '{' + track + '}';	
		if ( trackidx + 1 < totalTrackCount )
			tracks += ',';
	}
	
	response.append('['+tracks+']');
	return response;
}

cmdRemoveByIndex = function(path){
    var index = parseInt(path.substring(path.lastIndexOf("/")+1));
	if ( isNaN(index) ) { index = 0; }
	
	response = Amarok.Playlist.removeByIndex(index);
	response = new HandlerResponse(true);
	response.append('{"status":"OK","cmd":"playlist/removeByIndex","args":{"index":'+index+'},"results":{"totalTrackCount":'+Amarok.Playlist.totalTrackCount()+'}}');
	return response;
}

cmdPlaylistClear = function(path){
    Amarok.Playlist.clearPlaylist();
    response = new HandlerResponse();
    return response
}

cmdNext = function(path){
    Amarok.Engine.Next();
    return new HandlerResponse();  
}

cmdPrev = function(path){
    Amarok.Engine.Prev();
    return new HandlerResponse();
}

cmdPlay = function(path){
    Amarok.Engine.Play();
    return new HandlerResponse();
}

cmdPause = function(path){
    Amarok.Engine.Pause();
    return new HandlerResponse();
}

cmdPlayPause = function(path){
    if(Amarok.Engine.engineState() == 0)
        Amarok.Engine.Pause();
    else
        Amarok.Engine.Play();
    return new HandlerResponse();
}

cmdStop = function(path){
    Amarok.Engine.Stop(false);
    return new HandlerResponse();
}

cmdVolumeUp = function(path){
    step = parseInt(path.substring(path.lastIndexOf("/")+1));
    Amarok.Engine.IncreaseVolume(step);
    return new HandlerResponse();
}

cmdVolumeDown = function(path){
    step = parseInt(path.substring(path.lastIndexOf("/")+1));
    Amarok.Engine.DecreaseVolume(step);
    return new HandlerResponse();
}

cmdMute = function(path){
    Amarok.Engine.Mute()
    return new HandlerResponse();
}

cmdPlayByIndex = function(path){
    index = parseInt(path.substring(path.lastIndexOf("/")+1));
    Amarok.Playlist.playByIndex(index);
    return new HandlerResponse();
}

cmdSetPosition = function(path){
    position = parseInt(path.substring(path.lastIndexOf("/")+1));
    Amarok.Engine.Seek(position);
    return new HandlerResponse();
}


/* Collection commands */

getCollectionAllArtists = function(path){
    response = new HandlerResponse();
    artists = "";
    artistsQuery = Amarok.Collection.query("SELECT name, id FROM artists ORDER BY name;");
    for(artistidx=0; artistidx<artistsQuery.length; artistidx++){		 
		artist = artistsQuery[artistidx++].toString().replace(/(\r\n|\n|\r)/gm," ");
		artistId = artistsQuery[artistidx].toString().replace(/(\r\n|\n|\r)/gm," ");
		tracks = Amarok.Collection.query('SELECT count(*) FROM tracks WHERE artist = '+artistId+';').toString().replace(/(\r\n|\n|\r)/gm," ");
		albums = Amarok.Collection.query('SELECT count(*) FROM albums WHERE artist = '+artistId+';').toString().replace(/(\r\n|\n|\r)/gm," ");
		if (artist.length>0){
			artists += artistId + '\n' + artist + '\n' + tracks + '\n' + albums + '\n';
		}        
    }
    response.append(artists);
    return response;
}

getCollectionAllArtistsJSON = function(path){
    response = new HandlerResponse();
    artists = "";
    artistsQuery = Amarok.Collection.query("SELECT name, id FROM artists ORDER BY name;");
    for(artistidx=0; artistidx<artistsQuery.length; artistidx++){		 
		artist = artistsQuery[artistidx++].toString().replace(/(\r\n|\n|\r)/gm," ");
		artistId = artistsQuery[artistidx].toString().replace(/(\r\n|\n|\r)/gm," ");
		tracks = Amarok.Collection.query('SELECT count(*) FROM tracks WHERE artist = '+artistId+';').toString().replace(/(\r\n|\n|\r)/gm," ");
		albums = Amarok.Collection.query('SELECT count(*) FROM albums WHERE artist = '+artistId+';').toString().replace(/(\r\n|\n|\r)/gm," ");
		artists += '{"id":' + artistId + ',"name":' + ( artist.length == 0 ? '"Unknown Artist"' : '"' + jsonEscape(artist) + '"' ) + ',"tracks":'+ tracks + ',"albums":'+ albums +'}';
		if (artistidx+1<artistsQuery.length) {
			artists += ",";
		}       
    }
    response.append('['+artists+']');
    return response;
}

getCollectionTracksByArtistId = function(path){
    artistId = parseInt(path.substring(path.lastIndexOf("/")+1));
    trackQuery = Amarok.Collection.query('SELECT id, title, album FROM tracks WHERE artist = '+artistId+';');
    artistName = Amarok.Collection.query('SELECT name FROM artists WHERE id = '+artistId+';');
    trackCount = trackQuery.length;
    result = "";
    for(trackidx = 0; trackidx < trackCount; trackidx+=3){
	albumName = Amarok.Collection.query('SELECT name FROM albums WHERE id = '+trackQuery[trackidx+2]+';');
	trackId = trackQuery[trackidx].toString();
	trackTitle = trackQuery[trackidx+1];
	
	if(albumName=="") albumName = " ";
	if(trackTitle=="") trackTitle = " ";

	result += trackId.toString().replace(/(\r\n|\n|\r)/gm," ") + '\r';
	result += artistName.toString().replace(/(\r\n|\n|\r)/gm," ") + '\r';
	result += shorten(trackTitle, SHORTEN2).toString().replace(/(\r\n|\n|\r)/gm," ") + '\r';
	result += shorten(albumName, SHORTEN2).toString().replace(/(\r\n|\n|\r)/gm," ") + '\r';
	result += '\n';	
    }
    response = new HandlerResponse();
    response.append(result);
    return response
}

getCollectionTracksByArtistIdJSON = function(path){
    artistId = parseInt(path.substring(path.lastIndexOf("/")+1));
    trackQuery = Amarok.Collection.query('SELECT id, title, album FROM tracks WHERE artist = '+artistId+' ORDER BY album, tracknumber'+';');
    artistName = Amarok.Collection.query('SELECT name FROM artists WHERE id = '+artistId+';');
    trackCount = trackQuery.length;
    result = "";
    for(trackidx = 0; trackidx < trackCount; trackidx+=3){
		albumName = Amarok.Collection.query('SELECT name FROM albums WHERE id = '+trackQuery[trackidx+2]+';');
		trackId = trackQuery[trackidx].toString();
		trackTitle = trackQuery[trackidx+1];
		
		if(albumName=="") albumName = "Unknown album";
		if(trackTitle=="") trackTitle = "Unknown track";
		
		result += '{';
		result += '"track":"' + trackTitle + '",';
		result += '"trackID":' + trackId + ',';
		result += '"albumName":"' + albumName + '"';
		result += '}';
		if(trackidx+3 < trackCount){
			result += ',';
		}

    }
    response = new HandlerResponse();
    response.append('{"artist":"'+ artistName + '","artistID":' + artistId + ',"songs":['+ result +']}');
    return response
}

getCollectionAlbumsByArtistId = function(path){
    artistId = parseInt(path.substring(path.lastIndexOf("/")+1));
    trackQuery = Amarok.Collection.query('SELECT name FROM albums WHERE artist = '+artistId+';');
    trackCount = trackQuery.length;
    result = "";    
    
    for(trackidx = 0; trackidx < trackCount; trackidx+=1){
	trackId = trackQuery[trackidx].toString().replace(/(\r\n|\n|\r)/gm," ");
	result += trackId.toString().replace(/(\r\n|\n|\r)/gm," ") + '\r';
	result += '\n';	
    }
    response = new HandlerResponse();
    response.append(result);
    return response
}


getLyrics = function(path){
	trackId = parseInt(path.substring(path.lastIndexOf("/")+1));
	
	track = Amarok.Playlist.trackAt(trackId);
	artistID =  Amarok.Collection.query("SELECT id FROM artists WHERE name = '"+Amarok.Collection.escape(track.artist)+"';");
	trackURLID = Amarok.Collection.query("SELECT url FROM tracks WHERE artist = '"+artistID+"' AND title = '"+ track.title.toString() +"';");

	if (trackURLID.length > 0) {
		lyric = Amarok.Collection.query('SELECT lyrics FROM lyrics WHERE url = ' + trackURLID[0] + ';');
		response = new HandlerResponse(true);
		statusX = lyric.toString()+'\n';
		response.append(statusX);
		return response;
	}
	else {
		response = new HandlerResponse(true);
		statusX = "";
		response.append(statusX);
		return response;
	}
}

getCover = function(path){
    
    dimen = parseInt(path.substring(path.lastIndexOf("/")+1));
    rest = path.substring(0, path.lastIndexOf("/"));
    trackId = parseInt(rest.substring(rest.lastIndexOf("/")+1));

    response = new HandlerResponse(true);

	if ( Amarok.Playlist.trackAt(trackId).imageUrl != '' ) {
		response.setMimeType('image/png');
		response.append(pixmapToPNG(Amarok.Playlist.trackAt(trackId).imagePixmap(),dimen));
	}
		
	return response;
}


cmdCollectionPlayByTrackId = function(path){
    trackId = parseInt(path.substring(path.lastIndexOf("/")+1));
    trackURL = Amarok.Collection.query('SELECT rpath FROM urls LEFT JOIN tracks ON urls.id = tracks.url WHERE tracks.id = '+trackId+';');
    trackURL2 = Amarok.Collection.query('SELECT lastmountpoint FROM devices LEFT JOIN (urls LEFT JOIN tracks ON urls.id = tracks.url) ON devices.id = urls.deviceid WHERE tracks.id = '+trackId+';');
    Amarok.Playlist.addMedia(new QUrl('file://'+ trackURL2[0] + trackURL[0].substring(1)));    
    Amarok.Playlist.playByIndex(Amarok.Playlist.totalTrackCount()-1);
    response = new HandlerResponse();
    return response;
}

cmdCollectionEnqueue = function(path) {
    req_len = path.split("/").length;
    req_splitted = path.split("/");
    for(i = 2; i < req_len; i++) {
	trackId = req_splitted[i];
	trackURL = Amarok.Collection.query('SELECT rpath FROM urls LEFT JOIN tracks ON urls.id = tracks.url WHERE tracks.id = '+trackId+';');
	trackURL2 = Amarok.Collection.query('SELECT lastmountpoint FROM devices LEFT JOIN (urls LEFT JOIN tracks ON urls.id = tracks.url) ON devices.id = urls.deviceid WHERE tracks.id = '+trackId+';');
	Amarok.Playlist.addMedia(new QUrl('file://'+ trackURL2[0] + trackURL[0].substring(1)));
    }
    response = new HandlerResponse();
    return response;
}

getCollectionSearchAll = function(path){
    response = new HandlerResponse();
    result = "";
    querystring = decodeURIComponent(path.substring(path.lastIndexOf("/")+1));
    AllQuery = Amarok.Collection.query('SELECT tracks.id, artists.name, tracks.title, albums.name FROM (artists LEFT JOIN tracks ON artists.id = tracks.artist) LEFT JOIN albums ON tracks.album = albums.id WHERE UPPER(artists.name) LIKE UPPER("%' + querystring + '%") OR UPPER(tracks.title) LIKE UPPER("%' + querystring + '%") OR UPPER(albums.name) LIKE UPPER("%' + querystring + '%")');    /* search for artists */
    for(i = 0; i < AllQuery.length; i+=4){
	trackId = AllQuery[i].toString();
	artistName = AllQuery[i+1]
	trackTitle = AllQuery[i+2];
	albumName = AllQuery[i+3];	
	
	if(albumName=="") albumName = " ";
	if(trackTitle=="") trackTitle = " ";
	if(artistName=="") artistName = " ";

	result += trackId + '\r';
	result += artistName + '\r';
	result += shorten(trackTitle, SHORTEN2) + '\r';
	result += shorten(albumName, SHORTEN2) + '\r';
	result += '\n';	
    }
    response.append(result);
    return response
}

getCollectionSearchAllJSON = function(path){
    response = new HandlerResponse();
    result = "";
    querystring = decodeURIComponent(path.substring(path.lastIndexOf("/")+1));
    AllQuery = Amarok.Collection.query('SELECT tracks.id, artists.name, tracks.title, albums.name FROM (artists LEFT JOIN tracks ON artists.id = tracks.artist) LEFT JOIN albums ON tracks.album = albums.id WHERE UPPER(artists.name) LIKE UPPER("%' + querystring + '%") OR UPPER(tracks.title) LIKE UPPER("%' + querystring + '%") OR UPPER(albums.name) LIKE UPPER("%' + querystring + '%")');    /* search for artists */
    for(i = 0; i < AllQuery.length; i+=4){
	trackId = AllQuery[i].toString();
	artistName = AllQuery[i+1]
	trackTitle = AllQuery[i+2];
	albumName = AllQuery[i+3];	
	
	if(albumName=="") albumName = " ";
	if(trackTitle=="") trackTitle = " ";
	if(artistName=="") artistName = " ";

	result += '{';
	result += '"artistName":"' + jsonEscape(artistName) + '",';
	result += '"track":"' + jsonEscape(trackTitle) + '",';
	result += '"trackID":' + trackId + ',';
	result += '"albumName":"' + jsonEscape(albumName) + '"';
	result += '}';
	if(i+4 <  AllQuery.length){
		result += ',';
	}
	
    }
    response.append('['+result+']');
    return response
}
