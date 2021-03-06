package io.fata.polyfill.audio;

import java.math.BigInteger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnTimedMetaDataAvailableListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.TimedMetaData;
import android.media.TimedText;
import android.media.PlaybackParams;
import android.media.AudioManager;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;

public class RNAudioPlayer {

  protected final String TAG = "ReactNative";

  protected static RNAudioCache cache;
  protected static int __id__ = 1;

  protected int id;
  protected MediaPlayer player = new MediaPlayer();
  protected ReactContext context;
  protected WritableMap data = Arguments.createMap();
  protected Timer timer;
  protected PlaybackParams params = new PlaybackParams();
  protected Boolean loaded = false;

  protected void emitEvent(String type) {
    WritableMap map = Arguments.createMap();
    map.merge(this.data);
    map.putInt("audioId", this.id);
    map.putString("type", type);

    RCTDeviceEventEmitter emitter = context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    emitter.emit("RNAudioEvent", map);
  }
  public RNAudioPlayer(ReactContext context) {
    if(cache == null) {
      Context appContext = context.getApplicationContext();
      cache = new RNAudioCache(appContext);
    }
    this.context = context;
    this.id = __id__++;

    // default settings
    this.setAudioTracks("");
    this.setAutoplay(false);
    this._setBuffered(false);
    // this.setController(false);
    this.setControls(false);
    // this.setCrossOrigin("");
    this._setCurrentSrc("");
    this.setCurrentTime(0.0);
    this.setDefaultMuted(false);
    this.setDefaultPlaybackRate(1.0);
    this._setDuration(0.0);
    this._setEnded(false);
    this._setError("");
    this.setLoop(false);
    this.setMediaGroup("");
    this.setMuted(false);
    this._setNetworkState("");
    this.setPaused(false);
    this.setPlaybackRate(1.0);
    this._setPlayed(false);
    this.setPreload("auto");
    this._setSeekable(false);
    this._setSeeking(false);
    // this.setSource("");
    this.setTextTracks("");
    this.setVolume(1.0);
  }
  protected void setData(String key, Double value) {
    this.data.putDouble(key, value);
  }
  protected void setData(String key, String value) {
    this.data.putString(key, value);
  }
  protected void setData(String key, Boolean value) {
    this.data.putBoolean(key, value);
  }
  protected double getDouble(String key) {
    if(this.data.hasKey(key) == false) return 0.0;
    return this.data.getDouble(key);
  }
  protected String getString(String key) {
    if(this.data.hasKey(key) == false) return "";
    return this.data.getString(key);
  }
  protected Boolean getBoolean(String key) {
    if(this.data.hasKey(key) == false) return false;
    return this.data.getBoolean(key);
  }
  public int getId() {
    return this.id;
  }

  public void addTextTrack(String v) {
  }
  public Boolean canPlayType(String mediaType) {
    return false;
  }
  public void load() {
    this._load();
    // if(this.getBoolean("autoplay")) {
    //   Log.v(TAG, "autoplay: true");
    //   this.play();
    // }
  }
  protected void _load() {
    String source = this.getString("src");
    String currentSource = this.getString("currenSrc");

    // dispose
    if(loaded) {
      loaded = false;
      player.reset();
    }

    Log.v(TAG, "LOADING:" + source);
    _setCurrentSrc(source);

    if(source.equals("")) return;

    loaded = true;

    final RNAudioPlayer self = this;

    // event
    player.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
      @Override
      public synchronized void onBufferingUpdate(MediaPlayer mp, int percent) {
        self.emitEvent("progress");
      }
    });
    player.setOnCompletionListener(new OnCompletionListener() {
      @Override
      public synchronized void onCompletion(MediaPlayer mp) {
        self.emitEvent("ended");
        if(self.getBoolean("loop")) self.play();
      }
    });
    player.setOnErrorListener(new OnErrorListener() {
      @Override
      public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        switch(extra) {
          case MediaPlayer.MEDIA_ERROR_IO:
            self._setError("MEDIA_ERROR_IO");
            self.emitEvent("error");
            break;
          case MediaPlayer.MEDIA_ERROR_MALFORMED:
            self._setError("MEDIA_ERROR_MALFORMED");
            self.emitEvent("error");
            break;
          case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            self._setError("MEDIA_ERROR_UNSUPPORTED");
            self.emitEvent("error");
            break;
          case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
            self._setError("MEDIA_ERROR_TIMED_OUT");
            self.emitEvent("error");
            break;
        }
        return true;
      }
    });
    player.setOnInfoListener(new OnInfoListener() {
      @Override
      public synchronized boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch(what) {
          case MediaPlayer.MEDIA_INFO_UNKNOWN:
            break;
          case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
            break;
          case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
            break;
          case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            self._setBuffered(false);
            break;
          case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            self._setBuffered(true);
            break;
          case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            break;
          case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            self._setSeekable(false);
            break;
          case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            self.emitEvent("loadedmetadata");
            break;
          case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
            break;
          case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
        }
        return true;
      }
    });
    player.setOnSeekCompleteListener(new OnSeekCompleteListener() {
      @Override
      public synchronized void onSeekComplete(MediaPlayer mp) {
        self._setSeeking(false);
        self.emitEvent("seeked");
      }
    });
    player.setOnTimedMetaDataAvailableListener(new OnTimedMetaDataAvailableListener() {
      @Override
      public synchronized void onTimedMetaDataAvailable(MediaPlayer mp, TimedMetaData data) {
      }
    });
    player.setOnTimedTextListener(new OnTimedTextListener() {
      @Override
      public synchronized void onTimedText(MediaPlayer mp, TimedText text) {
      }
    });

    try {
      setDataSource(source);
      this.emitEvent("loadstart");

      player.prepare();

      int duration = player.getDuration();
      if(duration != -1) {
        self._setSeekable(true);
        self._setDuration((double)duration / 1000.0);
      }
      self.emitEvent("loadeddata");
      self.emitEvent("canplay");

    } catch (Exception e) {
      Log.e(TAG, "Exception", e);
      this._setError(e.getMessage());
      this.emitEvent("error");
      return;
    }
  }
  protected void play(double pos) {

    Log.v(TAG, "play.start:" + pos);

    this.setCurrentTime(pos);

    try {
      player.start();

      this.emitEvent("play");
      this.emitEvent("playing");

      final RNAudioPlayer self = this;

      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          if(player.isPlaying()) {
            self.setData("currentTime", player.getCurrentPosition() / 1000.0);
            self.emitEvent("timeupdate");
          } else {
            timer.cancel();
            timer.purge();
          }
        }
      }, 0, 1000);
    } catch(IllegalStateException e) {
    }
  }
  public void play() {

    if(!loaded) this._load();

    if(player.isPlaying()) return;

    double pos = player.getCurrentPosition() / 1000.0;

    // volume
    if(this.getBoolean("defaultMuted") == false) {
      float volume = (float)this.getDouble("volume");
      player.setVolume(volume, volume);
    }

    // playbackRate
    try {
      params.setSpeed((float)this.getDouble("defaultPlaybackRate"));
      player.setPlaybackParams(params);
      this.emitEvent("ratechange");
    } catch(IllegalStateException e) {
    }


    play(pos);
  }
  public void pause() {
    player.pause();
    this.setData("paused", true);
    this.emitEvent("pause");
  }
  public void setAudioTracks(String v) {
    this.setData("audioTracks", v);
  }
  public void setCache(Boolean v) {
    this.setData("cache", v);
  }
  public void setAutoplay(Boolean v) {
    this.setData("autoplay", v);
  }
  protected void _setBuffered(Boolean v) {
    this.setData("buffered", v);
  }
  // public void setController(Boolean v) {
  //   this.setData("controller", v);
  // }
  public void setControls(Boolean v) {
    this.setData("controls", v);
    // TODO
  }
  // public void setCrossOrigin(String v) {
  //   this.setData("crossOrigin", v);
  // }
  protected void _setCurrentSrc(String v) {
    this.setData("currentSrc", v);
  }
  public void setCurrentTime(double v) {
    this.setData("currentTime", v);
    if(this.getBoolean("seekable") == false)  return;
    player.seekTo((int)(v * 1000.0f));
    this._setSeeking(true);
    this.emitEvent("seeking");
  }
  public void setDefaultMuted(Boolean v) {
    this.setData("defaultMuted", v);
  }
  public void setDefaultPlaybackRate(double v) {
    this.setData("defaultPlaybackRate", v);
  }
  protected void _setDuration(double v) {
    double current = this.getDouble("duration");
    if(current != v) {
      this.setData("duration", v);
      this.emitEvent("durationchange");
    }
    Log.v(TAG, "duration:" + current + "->" + v);
  }
  protected void _setEnded(Boolean v) {
    this.setData("ended", v);
  }
  protected void _setError(String v) {
    this.setData("error", v);
  }
  public void setLoop(Boolean v) {
    this.setData("loop", v);
  }
  public void setMediaGroup(String v) {
    this.setData("mediaGroup", v);
  }
  public void setMuted(Boolean v) {
    this.setData("muted", v);
    if(v) {
      player.setVolume(0, 0);
    } else {
      float volume = (float)this.getDouble("volume");
      player.setVolume(volume, volume);
    }
  }
  protected void _setNetworkState(String v) {
    this.setData("networkState", v);
  }
  public void setPaused(Boolean v) {
    if(v) {
      this.pause();
    } else {
      double pos = player.getCurrentPosition() / 1000.0;
      this.play(pos);
    }
  }
  public void setPlaybackRate(double v) {
    this.setData("playbackRate", v);
    params.setSpeed((float)v);
    try {
      player.setPlaybackParams(params);
      this.emitEvent("ratechange");
    } catch(IllegalStateException e) {
    }
  }
  protected void _setPlayed(Boolean v) {
    this.setData("played", v);
  }
  public void setPreload(String v) {
    this.setData("preload", v);
    // TODO
  }
  protected void _setSeekable(Boolean v) {
    this.setData("seekable", v);
  }
  protected void _setSeeking(Boolean v) {
    this.setData("seeking", v);
  }
  public void setTextTracks(String v) {
    this.setData("textTracks", v);
  }
  public void setVolume(double v) {
    this.setData("volume", v);
    if(this.getBoolean("muted") == false) {
      player.setVolume((float)v, (float)v);
      this.emitEvent("volumechange");
    }
  }
  protected static String getMD5EncryptedString(String encTarget) {
    MessageDigest mdEnc = null;
    try {
      mdEnc = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } // Encryption algorithm
    mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
    String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
    while(md5.length() < 32) {
      md5 = "0"+md5;
    }
    return md5;
  }
  protected void setDataSource(String source) throws IOException {
    String filepath = cache.get(source);
    if(filepath != null) {
      File file = new File(filepath);
      FileInputStream stream = new FileInputStream(file);
      player.setDataSource(stream.getFD());
    }
  }
  public void setSource(String source) {
    this.setData("src", source);
    this._load();
  }
}
// TODO: abort  Fires when the loading of an audio/video is aborted
// canplay  Fires when the browser can start playing the audio/video
// TODO: canplaythrough Fires when the browser can play through the audio/video without stopping for buffering
// durationchange Fires when the duration of the audio/video is changed
// TODO: emptied  Fires when the current playlist is empty
// ended  Fires when the current playlist is ended
// error  Fires when an error occurred during the loading of an audio/video
// loadeddata Fires when the browser has loaded the current frame of the audio/video
// loadedmetadata Fires when the browser has loaded meta data for the audio/video
// loadstart  Fires when the browser starts looking for the audio/video
// pause  Fires when the audio/video has been paused
// play Fires when the audio/video has been started or is no longer paused
// playing  Fires when the audio/video is playing after having been paused or stopped for buffering
// progress Fires when the browser is downloading the audio/video
// ratechange Fires when the playing speed of the audio/video is changed
// seeked Fires when the user is finished moving/skipping to a new position in the audio/video
// seeking  Fires when the user starts moving/skipping to a new position in the audio/video
// TODO: stalled  Fires when the browser is trying to get media data, but data is not available
// TODO: suspend  Fires when the browser is intentionally not getting media data
// timeupdate Fires when the current playback position has changed
// volumechange Fires when the volume has been changed
// TODO: waiting  Fires when the video stops because it needs to buffer the next frame
