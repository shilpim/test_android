package com.example.fbpostsample;


import java.util.Arrays;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.facebook.AppEventsLogger;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;

public class MainActivity extends Activity {

	private TextView tvName = null;
	private final String PENDING_ACTION_BUNDLE_KEY = "com.example.fbpostsample.mainactivity:PendingAction";


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data, dialogCallback);
	}
	public static FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
		@Override
		public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
			Log.d("HelloFacebook", String.format("Error: %s", error.toString()));
		}

		@Override
		public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
			Log.d("HelloFacebook", "Success!");
		}
	};

	private UiLifecycleHelper uiHelper;
	@Override
	protected void onResume() {
		super.onResume();
		uiHelper.onResume();

		// Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
		// the onResume methods of the primary Activities that an app may be launched into.
		AppEventsLogger.activateApp(this);

		updateUI();
	}
	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();

		// Call the 'deactivateApp' method to log an app event for use in analytics and advertising
		// reporting.  Do so in the onPause methods of the primary Activities that an app may be launched into.
		AppEventsLogger.deactivateApp(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);

		outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
			pendingAction = PendingAction.valueOf(name);
		}

		tvName = (TextView) findViewById(R.id.tv_name);

		findViewById(R.id.btn_login).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				login();

			}
		});

		findViewById(R.id.btn_post).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				onClickPostStatusUpdate();

			}
		});

		// Can we present the share dialog for regular links?
		canPresentShareDialog = FacebookDialog.canPresentShareDialog(this,
				FacebookDialog.ShareDialogFeature.SHARE_DIALOG);
	}

	private enum PendingAction {
		NONE,
		POST_PHOTO,
		POST_STATUS_UPDATE
	}
	private boolean canPresentShareDialog;
	private void onClickPostStatusUpdate() {

		/*if (hasPublishPermission()) {
			Log.e("onClickPostStatusUpdate", "hasPermission");
			// We can do the action right away.
			postStatusUpdate();

		} else if (Session.getActiveSession() != null &&
				Session.getActiveSession().isOpened()) {
			Log.e("onClickPostStatusUpdate", " not hasPermission");
			// We need to get new permissions, then complete the action when we get called back.
			Session.getActiveSession().requestNewPublishPermissions(new Session
					.NewPermissionsRequest(this, PERMISSION));

		}
		else{
			Log.e("onClickPostStatusUpdate", " Session is null");
			login();
			//Session.openActiveSession(this, true, callback);
		}*/
		//postStatusUpdate();
		performPublish(PendingAction.POST_STATUS_UPDATE, canPresentShareDialog);
	}

	private FacebookDialog.ShareDialogBuilder createShareDialogBuilderForLink() {
		return new FacebookDialog.ShareDialogBuilder(this)
		.setName("Hello Dear")
		.setDescription("The 'Hello Facebook' sample application" +
				" showcases simple Facebook integration")
				.setLink("http://developers.facebook.com/");
	}

	private void postStatusUpdate() {
		if(Session.getActiveSession() != null){

			if (canPresentShareDialog) {
				FacebookDialog shareDialog = createShareDialogBuilderForLink().build();
				
				uiHelper.trackPendingDialogCall(shareDialog.present());
			} else if (hasPublishPermission()) {
				Log.e("postStatusUpdate", "hasPermission");
				final String message = "Shilpi is posting "+ (new Date().toString());

				Request request = Request
						.newStatusUpdateRequest(Session.getActiveSession(), message, null, null, 
								new Request.Callback() {
							@Override
							public void onCompleted(Response response) {
								Log.e("", ""+response.toString());
								//showPublishResult(message, response.getGraphObject(), response.getError());
							}
						});
				request.executeAsync();
			} else {
				Log.e("postStatusUpdate", "Not hasPermission");
				Session.getActiveSession().requestNewPublishPermissions(new Session
						.NewPermissionsRequest(this, PERMISSION));
				pendingAction = PendingAction.POST_STATUS_UPDATE;
			}
		}
	}


	private PendingAction pendingAction = PendingAction.NONE;
	private static final String PERMISSION = "publish_actions";

	private void performPublish(PendingAction action, boolean allowNoSession) {

		Session session = Session.getActiveSession();
		if (session != null) {
			pendingAction = action;
			if (hasPublishPermission()) {
				// We can do the action right away.
				handlePendingAction();
				return;
			} else if (session.isOpened()) {
				// We need to get new permissions, then complete the action when we get called back.
				session.requestNewPublishPermissions(new Session
						.NewPermissionsRequest(this, PERMISSION));
				return;
			}
		}
		else{
			Log.e("onClickPostStatusUpdate", " Session is null");
			login();
			//Session.openActiveSession(this, true, callback);
		}

		if (allowNoSession) {
			pendingAction = action;
			handlePendingAction();
		}
	}

	private boolean hasPublishPermission() {
		Session session = Session.getActiveSession();
		return session != null && session.getPermissions().contains("publish_actions");
	}
	@SuppressWarnings("incomplete-switch")
	private void handlePendingAction() {
		PendingAction previouslyPendingAction = pendingAction;
		// These actions may re-set pendingAction if they are still pending, but we assume they
		// will succeed.
		pendingAction = PendingAction.NONE;

		switch (previouslyPendingAction) {
		case POST_PHOTO:
			//postPhoto();
			break;
		case POST_STATUS_UPDATE:
			postStatusUpdate();
			break;
		}
	}



	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state, Exception exception) {

			Log.e("", ""+session);
			if(exception != null){
				exception.printStackTrace();
			}
			else if (session.isOpened()) {

				Request request = Request.newMeRequest(Session.getActiveSession(),
						new Request.GraphUserCallback() {
					@Override
					public void onCompleted(GraphUser user, Response response)
					{    
						MainActivity.this.user = user;
						updateUI();
						Log.e("UserName", ""+response.toString());
						if(user != null && Session.getActiveSession()!=null)
						{
							Log.e("UserName", ""+user.getName());
							//txtList.setText("\n"+user.getName());

						}
						if(response.getError() != null)
						{
							Log.e("Error","There is error in response");
						}
					}					
				});
				request.executeAsync(); 

				postStatusUpdate();
			}

			onSessionStateChange(session, state, exception);
		}
	};
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		if (pendingAction != PendingAction.NONE &&
				(exception instanceof FacebookOperationCanceledException ||
						exception instanceof FacebookAuthorizationException)) {
			new AlertDialog.Builder(MainActivity.this)
			.setTitle("Cancelled")
			.setMessage("Permission Not Granted")
			.setPositiveButton("ok", null)
			.show();
			pendingAction = PendingAction.NONE;
		} else if (state == SessionState.OPENED_TOKEN_UPDATED) {
			handlePendingAction();
		}
		//updateUI();
	}
	private void login(){

		Log.e("Response","Inside start session");
		Session session = new Session(this);
		Session.setActiveSession(session);
		session.openForRead(new Session.OpenRequest(this)
		.setCallback(callback)
		.setPermissions(Arrays.asList("user_friends")));



	}


	private GraphUser user;
	private void updateUI() {
		Session session = Session.getActiveSession();
		boolean enableButtons = (session != null && session.isOpened());

		if (enableButtons && user != null) {
			tvName.setText("Hello dear "+user.getFirstName());
		} 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
