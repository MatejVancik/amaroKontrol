package com.mv2studio.amarok.kontrol.ui.Activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.mv2studio.amarok.kontrol.App;
import com.mv2studio.amarok.kontrol.PlaylistAdapter;
import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.ConnectionMessage;
import com.mv2studio.amarok.kontrol.communication.ConnectorUpdateCallback;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokCommand;
import com.mv2studio.amarok.kontrol.communication.amarok.PlaylistUpdateCallback;
import com.mv2studio.amarok.kontrol.helpers.Storage;
import com.mv2studio.amarok.kontrol.photosbrowser.CacheCoversTask;
import com.mv2studio.amarok.kontrol.shared.Constants;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.ui.Fragment.BaseFragment;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Stack;

public class BaseActivity extends FragmentActivity {

    private BaseFragment[] mFragments;
    private static Stack<Integer> sFragmentStack = new Stack<>();
    private static int sCurrentFragment;

	protected SlidingMenu mSlidingMenu;
    private boolean isMenuAvailable;
	
	private SwipeListView mPlayListView;
	private static Bitmap sBackground;
	private RadioGroup mRadioGroup;
	private RelativeLayout mActionBarStatusLayout;
	private TextView mActionBarStatusText;
	private ImageButton mActionBarStatusButton, mActionBarSearch;
	private static AsyncTask sActionBarTask;

	public PlaylistAdapter mPlaylistAdapter;

    public void pushFragment(int fragmentId, Bundle arguments) {
        sFragmentStack.push(sCurrentFragment);
        sCurrentFragment = fragmentId;
        switchFragmentVisibility(fragmentId, arguments);
    }

    public void popFragment(int fragmentId) {
        while(sFragmentStack.pop() != fragmentId);
        sCurrentFragment = fragmentId;
        switchFragmentVisibility(fragmentId, null);
    }

    private void switchFragmentVisibility(int fragmentId, Bundle arguments) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.disallowAddToBackStack();
        BaseFragment fragmentToShow = null;

        for (BaseFragment fragment : mFragments) {
            transaction.hide(fragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            if(fragment.getId() == fragmentId) fragmentToShow = fragment;
        }

        if(arguments != null) fragmentToShow.setBundle(arguments);
        transaction.show(fragmentToShow);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
    }

	protected void onCreateActionBar() {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		ActionBar actionBar = getActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#aa000000")));
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setCustomView(R.layout.actionbar_navigaion);
		actionBar.setDisplayShowCustomEnabled(true);
		
		View actionBarRootView = actionBar.getCustomView();

		final ImageView collectionIcon = (ImageView) actionBarRootView.findViewById(R.id.nav_tab_collection_icon);
        final Animation off = AnimationUtils.loadAnimation(this, R.anim.scale_off);

		collectionIcon.startAnimation(off);
		
		mRadioGroup = (RadioGroup) actionBar.getCustomView().findViewById(R.id.nav_tab_radios);
		mActionBarStatusLayout = (RelativeLayout) actionBar.getCustomView().findViewById(R.id.actionbar_status);
		mActionBarStatusText = (TextView) mActionBarStatusLayout.findViewById(R.id.actionbar_status_message);
		mActionBarStatusButton = (ImageButton) mActionBarStatusLayout.findViewById(R.id.actionbar_status_button);
		mActionBarSearch = (ImageButton) actionBarRootView.findViewById(R.id.navigation_buttons_search);
		mActionBarSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pushFragment(R.id.fragSearch, null);
            }
        });
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onCreateActionBar();
		setContentView(R.layout.content_frame);

        if (!Storage.getBoolValue(Constants.CONST_UPDATE_SCRIPT)) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            View dialogLayout = View.inflate(this, R.layout.dialog_update, null);
            dialogLayout.findViewById(R.id.githubImage).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_GITHUB)));
                }
            });
            dialogBuilder.setView(dialogLayout);
            dialogBuilder.show();
            Storage.storeBoolValue(Constants.CONST_UPDATE_SCRIPT, true);
        }

        if(getResources().getBoolean(R.bool.IsPhone))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        FragmentManager fm = getSupportFragmentManager();
        mFragments = new BaseFragment[]{
                (BaseFragment) fm.findFragmentById(R.id.fragPlayNow),
                (BaseFragment) fm.findFragmentById(R.id.fragCollection),
                (BaseFragment) fm.findFragmentById(R.id.fragArtistDetail),
                (BaseFragment) fm.findFragmentById(R.id.fragSearch)
        };


        ActionBar actionBar = getActionBar();
        View actionBarRootView = actionBar.getCustomView();

        final ImageView playNowIcon = (ImageView) actionBarRootView.findViewById(R.id.nav_tab_play_now_icon);
        final ImageView collectionIcon = (ImageView) actionBarRootView.findViewById(R.id.nav_tab_collection_icon);

        final View playNowHightligh = actionBarRootView.findViewById(R.id.nav_tab_play_now_highlight);
        final View collectionHighlight = actionBarRootView.findViewById(R.id.nav_tab_collection_highlight);

        final Animation on = AnimationUtils.loadAnimation(this, R.anim.scale_on);
        final Animation off = AnimationUtils.loadAnimation(this, R.anim.scale_off);


        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.disallowAddToBackStack();

        for (BaseFragment fragment : mFragments) {
            transaction.hide(fragment);
        }

		if(savedInstanceState != null) {
			imageViewAnimatedChange(sBackground);
            if (sCurrentFragment != R.id.fragPlayNow) {
                ((RadioButton) mRadioGroup.findViewById(R.id.nav_tab_radio_collection)).setChecked(true);
            }
            transaction.show(fm.findFragmentById(sCurrentFragment));

            playNowHightligh.setVisibility(View.INVISIBLE);
            collectionHighlight.setVisibility(View.VISIBLE);
            collectionIcon.startAnimation(on);
            playNowIcon.startAnimation(off);
            setSearchButtonVisibile(true);
        } else {
            sCurrentFragment = R.id.fragPlayNow;
            transaction.show(fm.findFragmentById(R.id.fragPlayNow));
            collectionIcon.startAnimation(off);
        }

        transaction.commit();


        mPlaylistAdapter = new PlaylistAdapter(this, 0, new ArrayList<Song>());

		mSlidingMenu = new SlidingMenu(this);
		mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
		mSlidingMenu.setShadowDrawable(R.drawable.shsc);
        mSlidingMenu.setBehindOffsetRes(getResources().getBoolean(R.bool.IsSmallTablet) ?
                R.dimen.slidingmenu_tablet_offset : R.dimen.slidingmenu_offset);
		mSlidingMenu.setFadeDegree(0.35f);
		mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		mSlidingMenu.setMode(SlidingMenu.RIGHT);
		mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);

		if(isMenuAvailable = (findViewById(R.id.playlist_frame) == null)) {
			mSlidingMenu.setMenu(R.layout.playlist_layout);
		} else {
			mSlidingMenu.setSlidingEnabled(false);
		}

        View emptyView = findViewById(R.id.fragment_play_now_playlist_empty_view);
        mPlayListView = (SwipeListView) findViewById(R.id.playlistView);
        mPlayListView.setAdapter(mPlaylistAdapter);
		mPlayListView.setTag("reset");
		mPlayListView.setMultiChoiceModeListener(mMultiChoiceModeListener);
		mPlayListView.setSwipeListViewListener(mBaseSwipeListViewListener);
        mPlayListView.setEmptyView(emptyView);

        emptyView.setOnClickListener(onClickListener);
		findViewById(R.id.playlist_bottom_menu_refresh).setOnClickListener(onClickListener);
		findViewById(R.id.playlist_bottom_menu_jump).setOnClickListener(onClickListener);
        findViewById(R.id.playlist_bottom_menu_clear).setOnClickListener(onClickListener);

        mRadioGroup = (RadioGroup) actionBar.getCustomView().findViewById(R.id.nav_tab_radios);
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean isChecked = ((RadioButton) group.findViewById(checkedId)).isChecked();
                if (!isChecked) return;
                switch (checkedId) {
                    case R.id.nav_tab_radio_play_now:
                        playNowHightligh.setVisibility(View.VISIBLE);
                        collectionHighlight.setVisibility(View.INVISIBLE);
                        playNowIcon.startAnimation(on);
                        collectionIcon.startAnimation(off);

                        setSearchButtonVisibile(false);
                        // do not allow to "check" if it's already checked
                        if(sFragmentStack.size() >= 1) popFragment(R.id.fragPlayNow);
                        break;
                    case R.id.nav_tab_radio_collection:
                        playNowHightligh.setVisibility(View.INVISIBLE);
                        collectionHighlight.setVisibility(View.VISIBLE);
                        collectionIcon.startAnimation(on);
                        playNowIcon.startAnimation(off);

                        setSearchButtonVisibile(true);
                        pushFragment(R.id.fragCollection, null);
                        break;
                }
            }
        });

        refreshPlaylist();
	}

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.playlist_bottom_menu_refresh:
                case R.id.fragment_play_now_playlist_empty_view:
                    refreshPlaylist();
                    break;
                case R.id.playlist_bottom_menu_jump:
                    int currentSongIndex = mPlaylistAdapter.getIndexOfCurrentSong();
                    mPlayListView.smoothScrollToPositionFromTop(currentSongIndex, 200, 300);
                    break;
                case R.id.playlist_bottom_menu_clear:
                    App.getInstance().sendCommand(AmarokCommand.PLAYLIST_CLEAR);
                    refreshPlaylist();
                    break;
            }

        }
    };

    private AbsListView.MultiChoiceModeListener mMultiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mPlayListView.unselectedChoiceStates();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.playlist_back_view_remove) {
                mPlayListView.dismissSelected();
                return true;
            }
            new ExampleInterface(){

                @Override
                public void doTheJob() {

                }
            };
            return false;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

        }
    };

    private BaseSwipeListViewListener mBaseSwipeListViewListener = new BaseSwipeListViewListener() {
        @Override
        public void onMove(int position, float x) {
            int firstVisible = mPlayListView.getFirstVisiblePosition();
            int lastVisible = mPlayListView.getLastVisiblePosition();
            int count = lastVisible - firstVisible;
            for (int i = 0; i <= count; i++) {
                View backLayout = mPlayListView.getChildAt(i).findViewById(R.id.back);
                if (position - firstVisible == i) {
                    backLayout.setVisibility(View.VISIBLE);
                    backLayout.setAlpha(1f * (x / (float) backLayout.getWidth()));
                }
            }
            super.onMove(position, x);
        }

        @Override
        public void onOpened(int position, boolean toRight) {
            int firstVisible = mPlayListView.getFirstVisiblePosition();
            int lastVisible = mPlayListView.getLastVisiblePosition();
            int count = lastVisible - firstVisible;
            for (int i = 0; i <= count; i++) {
                View backLayout = mPlayListView.getChildAt(i).findViewById(R.id.back);
                View frontLayout = mPlayListView.getChildAt(i).findViewById(R.id.front);
                if (position - firstVisible == i && frontLayout.getX() > 0) {
                    backLayout.setVisibility(View.VISIBLE);
                    backLayout.setAlpha(1f);
                }
            }
        }

        @Override
        public void onClosed(int position, boolean fromRight) {
            int firstVisible = mPlayListView.getFirstVisiblePosition();
            int lastVisible = mPlayListView.getLastVisiblePosition();
            int count = lastVisible - firstVisible;
            for (int i = 0; i <= count; i++) {
                View backLayout = mPlayListView.getChildAt(i).findViewById(R.id.back);
                if (position - firstVisible == i) {
                    backLayout.setVisibility(View.GONE);
                    backLayout.setAlpha(0);

                    RelativeLayout frontLayout = (RelativeLayout) mPlayListView.getChildAt(i).findViewById(R.id.front);
                    frontLayout.setX(0f);
                }
            }
        }

        @Override
        public void onClickFrontView(int position) {
            Song clicked = (Song) mPlayListView.getItemAtPosition(position);
            AmarokCommand.PLAY_BY_INDEX.execute(clicked.getId() + "");
            if (isMenuAvailable && Prefs.closePlaylist) {
                mSlidingMenu.showContent(true);
            }
            mPlayListView.setFocusable(false);
            mPlayListView.setFocusable(true);
            mPlayListView.setFocusableInTouchMode(false);
            mPlayListView.setFocusableInTouchMode(true);
            super.onClickFrontView(position);
        }

        @Override
        public void onClickBackView(int position) {
            mPlayListView.closeOpenedItems();
            super.onClickBackView(position);
        }
    };

    private ConnectorUpdateCallback mConnectorUpdateCallback = new ConnectorUpdateCallback() {
        @Override public void onDataUnavailable(int errorCode, ConnectionMessage connectionMessage) { }
        @Override public void onPlayingStateChanged(PlayingState state) { }

        @Override
        public void onDataUpdated(Song song, PlayingState state) {
            if(song == null || song.getBlured() == null || sBackground == song.getBlured()) return;
            sBackground = song.getBlured();
            imageViewAnimatedChange(sBackground);
        }
    };

    public interface ExampleInterface {
        public void doTheJob();
    }

	@Override
	protected void onResume() {
		super.onResume();
		App.activityResumed();
        App.getInstance().registerConnectorCallback(mConnectorUpdateCallback);
        if(PlayingState.state != PlayingState.DOWN) imageViewAnimatedChange(sBackground);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		App.activityPaused();
        App.getInstance().unregisterConnectorCallback(mConnectorUpdateCallback);
        mPlaylistAdapter.unregisterConnectorCallback();
	}

	@Override
	public void onBackPressed() {
        try {
            if (sFragmentStack.size() == 1) {
                // if going to show play now just check radio button and it will take care of rest
                ((RadioButton)mRadioGroup.findViewById(R.id.nav_tab_radio_play_now)).setChecked(true);
            } else {
                popFragment(sFragmentStack.peek());
            }
        } catch (EmptyStackException e) {
            // now should quit app
            super.onBackPressed();
        }
    }


    @Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if(Prefs.useVolButtons) {
				if(event.getAction() == KeyEvent.ACTION_DOWN) { // Only on button down					
					switch (event.getKeyCode()) {
						case KeyEvent.KEYCODE_VOLUME_UP:
                            App.getInstance().sendCommand(AmarokCommand.VOL_UP.param(Prefs.volumeStep + ""));
							break;
						case KeyEvent.KEYCODE_VOLUME_DOWN:
                            App.getInstance().sendCommand(AmarokCommand.VOL_DOWN.param(Prefs.volumeStep + ""));
							break;
						default:
							return super.dispatchKeyEvent(event);
					}					
				} else if(event.getAction() == KeyEvent.ACTION_UP || 
						   event.getAction() == KeyEvent.ACTION_MULTIPLE) { // On button up/multiple
					switch (event.getKeyCode()) {
					case KeyEvent.KEYCODE_VOLUME_UP:
					case KeyEvent.KEYCODE_VOLUME_DOWN:
						return true; // ignore volume up/down on up/multiple action
					default:
						return super.dispatchKeyEvent(event);
					}
				}
				return true;
		}
		return super.dispatchKeyEvent(event);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.play_now, menu);
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
			return true;
		case R.id.menu_help:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(getLayoutInflater().inflate(R.layout.dialog_help, null));
			builder.show();
			break;
		case R.id.menu_feature:
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"depeha@gmail.com"});
			intent.putExtra(Intent.EXTRA_SUBJECT, "[amaroKontrol] Feature request");
			startActivity(Intent.createChooser(intent, "Send Email"));
			break;
		case R.id.menu_bug:
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"depeha@gmail.com"});
			intent.putExtra(Intent.EXTRA_SUBJECT, "[amaroKontrol] Bug Report");
			startActivity(Intent.createChooser(intent, "Send Email"));
		}
		return false;
	}

    public void recoverBackground() {
        imageViewAnimatedChange(sBackground);
    }

	@SuppressLint("NewApi")
	public void imageViewAnimatedChange(final Bitmap new_image) {
		final ImageView v1 = (ImageView)findViewById(R.id.main_background1);
		final ImageView v2 = (ImageView)findViewById(R.id.main_background2);

		Window window = getWindow();
		window.setBackgroundDrawable(new BitmapDrawable(getResources(), new_image));

	    final Animation anim_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
	    final Animation anim_in  = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

	    if(v1.getVisibility() == View.INVISIBLE) {
	    	v2.setVisibility(View.INVISIBLE);
	    	v2.startAnimation(anim_out);
	    	anim_out.setAnimationListener(new AnimationListener() {
	    		@Override public void onAnimationStart(Animation animation) {}
	            @Override public void onAnimationRepeat(Animation animation) {}
	            @Override public void onAnimationEnd(Animation animation) {v2.setImageBitmap(null);}
			});
	    	v1.setVisibility(View.VISIBLE);
	    	v1.setImageBitmap(new_image);
	    	v1.startAnimation(anim_in);
	    } else {
	    	v1.setVisibility(View.INVISIBLE);
	    	v1.startAnimation(anim_out);
	    	anim_out.setAnimationListener(new AnimationListener() {
	    		@Override public void onAnimationStart(Animation animation) {}
	            @Override public void onAnimationRepeat(Animation animation) {}
	            @Override public void onAnimationEnd(Animation animation) {v1.setImageBitmap(null);}
			});
	    	v2.setVisibility(View.VISIBLE);
	    	v2.setImageBitmap(new_image);
	    	v2.startAnimation(anim_in);
	    }
	    
	}
	

	public void showActionBarInfo(AsyncTask task) {
		sActionBarTask = task;
		Animation animation = new TranslateAnimation(0 , 0, -mActionBarStatusLayout.getHeight(), 0);
		animation.setDuration(500);
		mActionBarStatusLayout.startAnimation(animation);
		mActionBarStatusLayout.setVisibility(View.VISIBLE);
		mActionBarStatusButton.setVisibility(View.VISIBLE);
	}
	
	public void hideActionBarInfo(AsyncTask task) {
		if(sActionBarTask != task) return;
		if(mActionBarStatusLayout.getVisibility() == View.INVISIBLE) return;
		mActionBarStatusLayout.startAnimation(createHideAnimationForActionBarItem(mActionBarStatusLayout));
		sActionBarTask = null;
	}
	
	private Animation createHideAnimationForActionBarItem(final View view) {
		Animation animation = new TranslateAnimation(0 , 0, 0, -view.getHeight());
		animation.setDuration(500);
		animation.setAnimationListener(new AnimationListener() {
			@Override public void onAnimationStart(Animation animation) { }
			@Override public void onAnimationRepeat(Animation animation) { }
			@Override public void onAnimationEnd(Animation animation) { view.setVisibility(View.GONE); }
		});
		return animation;
	}
	
	public void setActionBarButtonInvisible(AsyncTask task) {
		if(sActionBarTask != task) return;
		mActionBarStatusButton.setVisibility(View.INVISIBLE);
	}
	
	public void setActionBarInfoText(String text, AsyncTask task) {
		if(sActionBarTask != task) return;
		mActionBarStatusText.setText(text);
	}
	
	public void setActionBarInfoCancelTask(final AsyncTask task) {
		mActionBarStatusButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                task.cancel(true);
            }
        });
	}
	
    private void setSearchButtonVisibile(boolean visible) {
        if (visible) {
            Animation animation = new TranslateAnimation(0 , 0, -mActionBarSearch.getHeight(), 0);
            animation.setDuration(500);
            mActionBarSearch.startAnimation(animation);
            mActionBarSearch.setVisibility(View.VISIBLE);
        } else {
            mActionBarSearch.startAnimation(createHideAnimationForActionBarItem(mActionBarSearch));
        }
    }

	public void updatePlaylistItemsPlayIcon() {
		try {
			int currentSongIndex = mPlaylistAdapter.getIndexOfCurrentSong();
			if(currentSongIndex > -1) {
				int firstVisible = mPlayListView.getFirstVisiblePosition();
				int lastVisible = mPlayListView.getLastVisiblePosition();
				int count = lastVisible - firstVisible;
				for(int i = 0; i < count; i++) {
					ImageView playingIcon = (ImageView) mPlayListView.getChildAt(i).findViewById(R.id.listSongPlayingIcon);
					playingIcon.setVisibility((currentSongIndex - firstVisible == i) ? View.VISIBLE : View.GONE);
				}				
			}
		} catch (NullPointerException ex) {}
	}
	
	public void refreshPlaylist() {
        App.getInstance().updatePlaylist(new PlaylistUpdateCallback() {
            @Override
            public void onPlaylistReceived(ArrayList<Song> list) {
                if(list.size() > 0)
                    CacheCoversTask.runTask(list.toArray(new Song[list.size()]));

                mPlaylistAdapter.clear();
                mPlaylistAdapter.addAll(list);

                View emptyView = ((View) mPlayListView.getParent()).findViewById(R.id.fragment_play_now_playlist_empty_view);

                if (list.size() > 0) {
                    emptyView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.VISIBLE);
                }

                mPlayListView.requestLayout();
            }
        });
	}
	
}
