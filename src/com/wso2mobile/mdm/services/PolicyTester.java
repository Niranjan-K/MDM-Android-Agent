package com.wso2mobile.mdm.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.wso2mobile.mdm.AlertActivity;
import com.wso2mobile.mdm.api.ApplicationManager;
import com.wso2mobile.mdm.api.DeviceInfo;
import com.wso2mobile.mdm.api.PhoneState;
import com.wso2mobile.mdm.api.WiFiConfig;
import com.wso2mobile.mdm.models.PInfo;
import com.wso2mobile.mdm.utils.CommonUtilities;
import com.wso2mobile.mdm.utils.ServerUtilities;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

public class PolicyTester {

	Context context;
	DevicePolicyManager devicePolicyManager;
	ApplicationManager appList;
	DeviceInfo deviceInfo;
	PhoneState deviceState;
	String policy;
	String usermessage = "";
	JSONObject returnJSON = new JSONObject();
	JSONArray finalArray = new JSONArray();
	boolean IS_ENFORCE = false;
	String ssid, password;
	int POLICY_MONITOR_TYPE_NO_ENFORCE_RETURN = 1;
	int POLICY_MONITOR_TYPE_NO_ENFORCE_MESSAGE_RETURN = 2;
	int POLICY_MONITOR_TYPE_ENFORCE_RETURN = 3;
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public PolicyTester(Context context, JSONArray recJArray, int type, String msgID) {
		this.context = context;
		devicePolicyManager = (DevicePolicyManager) context
				.getSystemService(Context.DEVICE_POLICY_SERVICE);
		appList = new ApplicationManager(context);
		deviceInfo = new DeviceInfo(context);
		deviceState = new PhoneState(context);
		
		if(type == POLICY_MONITOR_TYPE_NO_ENFORCE_RETURN){
			IS_ENFORCE = false;
		}else if(type == POLICY_MONITOR_TYPE_NO_ENFORCE_MESSAGE_RETURN){
			IS_ENFORCE = false;
		}else if(type == POLICY_MONITOR_TYPE_ENFORCE_RETURN){
			IS_ENFORCE = true;
		}else{
			IS_ENFORCE = false;
			type = POLICY_MONITOR_TYPE_NO_ENFORCE_MESSAGE_RETURN;
		}

		SharedPreferences mainPref = context.getSharedPreferences("com.mdm",
				Context.MODE_PRIVATE);
		policy = mainPref.getString("policy", "");

		try {
			JSONArray jArray = null;
			if(recJArray!=null){
				jArray = recJArray;
			}else{
				jArray = new JSONArray(policy);
			}
			Log.e("POLICY ARAY : ", jArray.toString());
			for (int i = 0; i < jArray.length(); i++) {
				JSONObject policyObj = (JSONObject) jArray.getJSONObject(i);
				if (policyObj.getString("data") != null
						&& policyObj.getString("data") != "") {
					testPolicy(policyObj.getString("code"),
							policyObj.getString("data"));
				}
			}
			
			JSONObject rootObj = new JSONObject();
			try {
				if(deviceInfo.isRooted()){
					rootObj.put("status", false);
				}else{
					rootObj.put("status", true);
				}
				rootObj.put("code", "notrooted");
				finalArray.put(rootObj);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Log.e("MONITOR POLICY : ",policy);
			Log.e("MONITOR USER MESSAGE : ",usermessage);
			//Display an alert to the user about policy violation
			if(policy!=null && policy !=""){
				if(usermessage!=null && usermessage!="" && type == POLICY_MONITOR_TYPE_NO_ENFORCE_MESSAGE_RETURN){
					Intent intent = new Intent(context, AlertActivity.class);
					intent.putExtra("message", usermessage);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
				}
			}
			
			returnJSON.put("code", CommonUtilities.OPERATION_POLICY_MONITOR);
			returnJSON.put("data", finalArray);
			
			Map<String, String> params = new HashMap<String, String>();
			params.put("code", CommonUtilities.OPERATION_POLICY_MONITOR);
			params.put("msgID", msgID);
			params.put("status", "200");
			params.put("data", finalArray.toString());
			
			ServerUtilities.pushData(params, context);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@SuppressWarnings("static-access")
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public boolean testPolicy(String code, String data) {
		if (code.equals(CommonUtilities.OPERATION_CLEAR_PASSWORD)) {
			ComponentName demoDeviceAdmin = new ComponentName(context,
					WSO2MobileDeviceAdminReceiver.class);
			JSONObject jobj = new JSONObject();
			// data = intent.getStringExtra("data");
			try {
				Map<String, String> params = new HashMap<String, String>();
				params.put("code", code);
				params.put("status", "200");

				if(IS_ENFORCE){
					devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
							DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
					devicePolicyManager.resetPassword("",
							DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
					devicePolicyManager.lockNow();
					devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
							DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
					jobj.put("status", true);
				}else{
					if(devicePolicyManager.getPasswordQuality(demoDeviceAdmin) != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED){
						jobj.put("status", false);
					}else{
						jobj.put("status", true);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				jobj.put("code", code);
				
				//finalArray.put(jobj);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			

		} else if (code.equals(CommonUtilities.OPERATION_WIFI)) {
			boolean wifistatus = false;
			JSONObject jobjc = new JSONObject();
			WiFiConfig config = new WiFiConfig(context);
			// data = intent.getStringExtra("data");
			JSONParser jp = new JSONParser();
			try {
				JSONObject jobj = new JSONObject(data);
				if(!jobj.isNull("ssid")){
					ssid = (String) jobj.get("ssid");
				}
				if(!jobj.isNull("password")){
					password = (String) jobj.get("password");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Map<String, String> inparams = new HashMap<String, String>();
			inparams.put("code", code);
			if(IS_ENFORCE){
			try {
				wifistatus = config.saveWEPConfig(ssid, password);
				jobjc.put("status", true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
			
			try {
				if(config.readWEPConfig(ssid)){
					jobjc.put("status", true);
				}else{
					jobjc.put("status", false);
					if(usermessage!=null && usermessage!=""){
						usermessage+="\nYou are not using company WIFI account, please change your WIFI configuration \n";
					}else{
						usermessage+="You are not using company WIFI account, please change your WIFI configuration \n";
					}
					
				}
				jobjc.put("code", code);
				
				finalArray.put(jobjc);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} else if (code.equals(CommonUtilities.OPERATION_DISABLE_CAMERA)) {
			ComponentName cameraAdmin = new ComponentName(context,
					WSO2MobileDeviceAdminReceiver.class);
			boolean camFunc = false;
			// data = intent.getStringExtra("data");
			JSONParser jp = new JSONParser();
			try {
				JSONObject jobj = new JSONObject(data);
				
				if (!jobj.isNull("function")
						&& jobj.get("function").toString()
								.equalsIgnoreCase("enable")) {
					camFunc = false;
				} else if (!jobj.isNull("function")
						&& jobj.get("function").toString()
								.equalsIgnoreCase("disable")) {
					camFunc = true;
				} else if (!jobj.isNull("function")) {
					camFunc = Boolean.parseBoolean(jobj.get("function")
							.toString());
				}

				
				Map<String, String> params = new HashMap<String, String>();
				params.put("code", code);
				params.put("status", "200");
				String cammode = "Disabled";
				if (camFunc) {
					cammode = "Disabled";
				} else {
					cammode = "Enabled";
				}

				if (IS_ENFORCE && (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)) {
					devicePolicyManager.setCameraDisabled(cameraAdmin, camFunc);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jobj = new JSONObject();
			try {
				if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					if(!camFunc){
						if(!devicePolicyManager.getCameraDisabled(cameraAdmin)){
							jobj.put("status", true);
						}else{
							jobj.put("status", false);
							/*if(usermessage!=null && usermessage!=""){
								usermessage+="\nYour camera should be deactivated according to the policy, please deactivate your camera\n";
							}else{
								usermessage+="Your camera should be deactivated according to the policy, please deactivate your camera \n";
							}*/
						}
					}else{
						if(devicePolicyManager.getCameraDisabled(cameraAdmin)){
							jobj.put("status", true);
						}else{
							jobj.put("status", false);
							if(usermessage!=null && usermessage!=""){
								usermessage+="\nYour camera should be deactivated according to the policy, please deactivate your camera\n";
							}else{
								usermessage+="Your camera should be deactivated according to the policy, please deactivate your camera \n";
							}
						}
					}
				}else{
					jobj.put("status", false);
					/*if(usermessage!=null && usermessage!=""){
						usermessage+="\nYour camera should be deactivated according to the policy, please deactivate your camera\n";
					}else{
						usermessage+="Your camera should be deactivated according to the policy, please deactivate your camera \n";
					}*/
				}
				jobj.put("code", code);
				
				finalArray.put(jobj);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} else if (code.equals(CommonUtilities.OPERATION_ENCRYPT_STORAGE)) {
			boolean encryptFunc = true;
			String pass = "";
			// data = intent.getStringExtra("data");
			JSONParser jp = new JSONParser();
			try {
				JSONObject jobj = new JSONObject(data);
				// pass = (String)jobj.get("password");
				if (!jobj.isNull("function")
						&& jobj.get("function").toString()
								.equalsIgnoreCase("encrypt")) {
					encryptFunc = true;
				} else if (!jobj.isNull("function")
						&& jobj.get("function").toString()
								.equalsIgnoreCase("decrypt")) {
					encryptFunc = false;
				} else if (!jobj.isNull("function")) {
					encryptFunc = Boolean.parseBoolean(jobj.get("function")
							.toString());
				}

				// ComponentName cameraAdmin = new ComponentName(this,
				// DemoDeviceAdminReceiver.class);
				ComponentName admin = new ComponentName(context,
						WSO2MobileDeviceAdminReceiver.class);
				Map<String, String> params = new HashMap<String, String>();
				params.put("code", code);
				
				if(IS_ENFORCE){
					if (encryptFunc
							&& devicePolicyManager.getStorageEncryptionStatus() != devicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED) {
						if (devicePolicyManager.getStorageEncryptionStatus() == devicePolicyManager.ENCRYPTION_STATUS_INACTIVE) {
							// devicePolicyManager.resetPassword(pass,
							// DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
							if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
								devicePolicyManager.setStorageEncryption(admin,
										encryptFunc);
								Intent intent = new Intent(
										DevicePolicyManager.ACTION_START_ENCRYPTION);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(intent);
							}
						}
					} else if (!encryptFunc
							&& devicePolicyManager.getStorageEncryptionStatus() != devicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED) {
						if (devicePolicyManager.getStorageEncryptionStatus() == devicePolicyManager.ENCRYPTION_STATUS_ACTIVE
								|| devicePolicyManager.getStorageEncryptionStatus() == devicePolicyManager.ENCRYPTION_STATUS_ACTIVATING) {
							if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
								devicePolicyManager.setStorageEncryption(admin,
										encryptFunc);
							}
						}
					}
				}
				if (devicePolicyManager.getStorageEncryptionStatus() != devicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED) {
					params.put("status", "200");
				} else {
					params.put("status", "400");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jobj = new JSONObject();
			try {
				jobj.put("code", code);
				if(encryptFunc){
					if(devicePolicyManager.getStorageEncryptionStatus()!= devicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED && devicePolicyManager.getStorageEncryptionStatus() != devicePolicyManager.ENCRYPTION_STATUS_INACTIVE){
						jobj.put("status", true);
					}else{
						jobj.put("status", false);	
						if(usermessage!=null && usermessage!=""){
							usermessage+="\nYour device should be encrypted according to the policy, please enable device encryption through device settings\n";
						}else{
							usermessage+="Your device should be encrypted according to the policy, please enable device encryption through device settings \n";
						}
					}
				}else{
					if(devicePolicyManager.getStorageEncryptionStatus()== devicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED || devicePolicyManager.getStorageEncryptionStatus() == devicePolicyManager.ENCRYPTION_STATUS_INACTIVE){
						jobj.put("status", true);
					}else{
						jobj.put("status", false);	
					}
				}
				finalArray.put(jobj);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else if (code.equals(CommonUtilities.OPERATION_MUTE)) {

			try {
				Map<String, String> params = new HashMap<String, String>();
				params.put("code", code);
				params.put("status", "200");
				if(IS_ENFORCE){
					muteDevice();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jobj = new JSONObject();
			try {
				jobj.put("code", code);
				if(isMuted()){
					jobj.put("status", true);
				}else{
					jobj.put("status", false);
					if(usermessage!=null && usermessage!=""){
						usermessage+="\nYour phone should be muted according to the policy, please mute your phone \n";
					}else{
						usermessage+="Your phone should be muted according to the policy, please mute your phone \n";
					}
				}
				finalArray.put(jobj);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} else if (code.equals(CommonUtilities.OPERATION_PASSWORD_POLICY)) {
			
			ComponentName demoDeviceAdmin = new ComponentName(context,
					WSO2MobileDeviceAdminReceiver.class);
			JSONObject jobjx = new JSONObject();
			int attempts, length, history, specialChars;
			String alphanumeric, complex;
			boolean b_alphanumeric=false, b_complex=false;
			long timout;
			Map<String, String> inparams = new HashMap<String, String>();
			// data = intent.getStringExtra("data");
			JSONParser jp = new JSONParser();
			try {
				JSONObject jobjpass = new JSONObject();
				jobjpass.put("code", CommonUtilities.OPERATION_CHANGE_LOCK_CODE);
				
				if(devicePolicyManager.isActivePasswordSufficient()){
					jobjpass.put("status", true);
					//finalArray.put(jobjpass);
				}else{
					jobjpass.put("status", false);
					//finalArray.put(jobjpass);
					if(usermessage!=null && usermessage!=""){
						 usermessage+="\nYour screen lock password doesn't meet current policy requirement. Please reset your passcode \n";
					}else{
						 usermessage+="Your screen lock password doesn't meet current policy requirement. Please reset your passcode \n";
					}
							/*Intent intent = new Intent(context, AlertActivity.class);
							intent.putExtra("message", "Your screen lock password doesn't meet current policy requirement. Please reset your passcode");
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);*/
				}
				
				JSONObject jobj = new JSONObject(data);
				if (!jobj.isNull("maxFailedAttempts")
						&& jobj.get("maxFailedAttempts") != null) {
					attempts = Integer.parseInt((String) jobj
							.get("maxFailedAttempts"));
					if(IS_ENFORCE){
					devicePolicyManager.setMaximumFailedPasswordsForWipe(
							demoDeviceAdmin, attempts);
					jobjx.put("status", true);
					}else{
						if(devicePolicyManager.getMaximumFailedPasswordsForWipe(demoDeviceAdmin) != attempts){
							jobjx.put("status", false);
						}else{
							jobjx.put("status", true);
						}
					}
				}

				if (!jobj.isNull("minLength") && jobj.get("minLength") != null) {
					length = Integer.parseInt((String) jobj.get("minLength"));
					if(IS_ENFORCE){
					devicePolicyManager.setPasswordMinimumLength(
							demoDeviceAdmin, length);
					jobjx.put("status", true);
					}else{
						if(devicePolicyManager.getPasswordMinimumLength(demoDeviceAdmin) != length){
							jobjx.put("status", false);
						}else{
							jobjx.put("status", true);
						}
					}
				}

				if (!jobj.isNull("pinHistory")
						&& jobj.get("pinHistory") != null) {
					history = Integer.parseInt((String) jobj.get("pinHistory"));
					if(IS_ENFORCE){
					devicePolicyManager.setPasswordHistoryLength(
							demoDeviceAdmin, history);
					jobjx.put("status", true);
					}else{
						if(devicePolicyManager.getPasswordHistoryLength(demoDeviceAdmin) != history){
							jobjx.put("status", false);
						}else{
							jobjx.put("status", true);
						}
					}
				}

				if (!jobj.isNull("minComplexChars")
						&& jobj.get("minComplexChars") != null) {
					specialChars = Integer.parseInt((String) jobj
							.get("minComplexChars"));
					if(IS_ENFORCE){
					devicePolicyManager.setPasswordMinimumSymbols(
							demoDeviceAdmin, specialChars);
					jobjx.put("status", true);
					}else{
						if(devicePolicyManager.getPasswordMinimumSymbols(demoDeviceAdmin) != specialChars){
							jobjx.put("status", false);
						}else{
							jobjx.put("status", true);
						}
					}
				}

				if (!jobj.isNull("requireAlphanumeric")
						&& jobj.get("requireAlphanumeric") != null) {
					
					if(jobj.get("requireAlphanumeric") instanceof String){
						alphanumeric = (String) jobj.get("requireAlphanumeric").toString();
						if (alphanumeric.equals("true")) {
							b_alphanumeric=true;
						}else{
							b_alphanumeric=false;
						}
					}else if(jobj.get("requireAlphanumeric") instanceof Boolean){
						b_alphanumeric =  jobj.getBoolean("requireAlphanumeric");
					}
					if (b_alphanumeric) {
						if(IS_ENFORCE){
						devicePolicyManager
								.setPasswordQuality(
										demoDeviceAdmin,
										DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);
						jobjx.put("status", true);
						}else{
							if(devicePolicyManager.getPasswordQuality(demoDeviceAdmin) != DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC){
								jobjx.put("status", false);
							}else{
								jobjx.put("status", true);
							}
						}
					}else{
						if(devicePolicyManager.getPasswordQuality(demoDeviceAdmin) == DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC){
							jobjx.put("status", false);
						}else{
							jobjx.put("status", true);
						}
					}
				}

				if (!jobj.isNull("allowSimple")
						&& jobj.get("allowSimple") != null) {
					
					if(jobj.get("allowSimple") instanceof String){
						complex = (String) jobj.get("allowSimple").toString();
						if (complex.equals("true")) {
							b_complex=true;
						}else{
							b_complex=false;
						}
					}else if(jobj.get("allowSimple") instanceof Boolean){
						b_complex =  jobj.getBoolean("allowSimple");
					}

					if (!b_complex) {
						if(IS_ENFORCE){
						devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
								DevicePolicyManager.PASSWORD_QUALITY_COMPLEX);
						jobjx.put("status", true);
						}else{
							if(devicePolicyManager.getPasswordQuality(demoDeviceAdmin) != DevicePolicyManager.PASSWORD_QUALITY_COMPLEX){
								jobjx.put("status", false);
							}else{
								jobjx.put("status", true);
							}
						}
					}else{
						if(devicePolicyManager.getPasswordQuality(demoDeviceAdmin) == DevicePolicyManager.PASSWORD_QUALITY_COMPLEX){
							jobjx.put("status", false);
						}else{
							jobjx.put("status", true);
						}
					}
				}

				if (!jobj.isNull("maxPINAgeInDays")
						&& jobj.get("maxPINAgeInDays") != null) {
					int daysOfExp = Integer.parseInt((String) jobj
							.get("maxPINAgeInDays"));
					timout = (long) (daysOfExp * 24 * 60 * 60 * 1000);
					if(IS_ENFORCE){
					devicePolicyManager.setPasswordExpirationTimeout(
							demoDeviceAdmin, timout);
					jobjx.put("status", true);
					}else{
						if(devicePolicyManager.getPasswordExpirationTimeout(demoDeviceAdmin) != timout){
							jobjx.put("status", false);
						}else{
							jobjx.put("status", true);
						}
					}
				}
				
				if(devicePolicyManager.isActivePasswordSufficient()){
					jobjx.put("status", true);
				}else{
					jobjx.put("status", false);
				}
				
				
				inparams.put("code", code);
				inparams.put("status", "200");

			} catch (Exception e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
			}
			
			try {
				jobjx.put("code", code);
				
				finalArray.put(jobjx);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}else if (code
				.equals(CommonUtilities.OPERATION_BLACKLIST_APPS)) {
			ArrayList<PInfo> apps = appList.getInstalledApps(false); /*
																	 * false =
																	 * no system
																	 * packages
																	 */
			// String apps[] = appList.getApplicationListasArray();
			JSONArray jsonArray = new JSONArray();
			int max = apps.size();
			if (max > 10) {
				//max = 10;
			}
			String apz = "";
			Boolean flag = true;
			JSONArray jArray = null;
			int appcount = 1;
			try{
				jArray = new JSONArray(data);
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject appObj = (JSONObject) jArray
							.getJSONObject(i);
					String identity = (String) appObj.get("identity");
					for (int j = 0; j < max; j++) {
						JSONObject jsonObj = new JSONObject();
						try {
							jsonObj.put("name", apps.get(j).appname);
							jsonObj.put("package", apps.get(j).pname);
							if(identity.trim().equals(apps.get(j).pname)){
								jsonObj.put("notviolated", false);
								flag = false;
								jsonObj.put("package", apps.get(j).pname);
								if(i<(jArray.length()-1)){
									if(apps.get(j).appname!=null){
										apz += appcount+". "+apps.get(j).appname + "\n";
										appcount++;
									}
										
								}else{
									if(apps.get(j).appname!=null){
										apz += appcount+". "+apps.get(j).appname;
										appcount++;
									}
								}
							}else{
								jsonObj.put("notviolated", true);
							}
							
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						jsonArray.put(jsonObj);
					}
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			
			/*
			 * for(int i=0;i<apps.length;i++){ jsonArray.add(apps[i]); }
			 */
			JSONObject appsObj = new JSONObject();
			try {
				//appsObj.put("data", jsonArray);
				appsObj.put("status", flag);
				appsObj.put("code", code);
				finalArray.put(appsObj);
				if(apz!=null || !apz.trim().equals("")){
					if(usermessage!=null && usermessage!=""){
						usermessage+="\nFollowing apps are blacklisted by your MDM Admin, please remove them \n\n"+apz;
					}else{
						usermessage+="Following apps are blacklisted by your MDM Admin, please remove them \n\n"+apz;
					}
					/*Intent intent = new Intent(context, AlertActivity.class);
					intent.putExtra("message", "Following apps are blacklisted by your MDM Admin, please remove them \n\n"+apz);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);*/
				}
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		
		return true;
	}
	
	/**
	 * Set WiFi
	 */
	/*public boolean setWifi(String SSID, String password) {

		WifiConfiguration wc = new WifiConfiguration();

		wc.SSID = "\"{SSID}\"".replace("{SSID}", SSID);
		wc.preSharedKey = "\"{PRESHAREDKEY}\"".replace("{PRESHAREDKEY}",
				password);

		wc.status = WifiConfiguration.Status.ENABLED;
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		int netId = wifi.addNetwork(wc);
		wifi.enableNetwork(netId, true);
		
		if (wifi.getConnectionInfo().getSSID()!= null && wifi.getConnectionInfo().getSSID().equals(SSID)){
    		Log.i("Hub", "WiFi is enabled AND active !");
    		Log.i("Hub", "SSID = "+wifi.getConnectionInfo().getSSID());
    		return true;
		}else{
			Log.i("Hub", "NO WiFi");
			return false;
		}
	}*/

	/**
	 * Mute the device
	 */
	private void muteDevice() {
		Log.v("MUTING THE DEVICE : ", "MUTING");
		AudioManager audioManager = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		Log.v("VOLUME : ",
				"" + audioManager.getStreamVolume(AudioManager.STREAM_RING));
		audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
		Log.v("VOLUME AFTER: ",
				"" + audioManager.getStreamVolume(AudioManager.STREAM_RING));

	}
	
	private boolean isMuted(){
		AudioManager audioManager = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		if(audioManager.getStreamVolume(AudioManager.STREAM_RING)!=0){
			return false;
		}else{
			return true;
		}
	}
}
