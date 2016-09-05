package ntu.fyp.sjy.securitycontroller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import ntu.fyp.sjy.policymonitoring.GenerateMonitor;
import ntu.fyp.sjy.policymonitoring.Print;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.pem.Event;
import android.pem.MonitorAPI;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


@SuppressLint("WorldReadableFiles")
public class MainActivity extends FragmentActivity {
	
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this
	 * becomes too memory intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	
	private MonitorAPI monitor;
	private BasicFragment bif = null;
	private AdvancedFragment aif = null;
	
	//basicList to store the resources set to be protected in basic interface
    ArrayList<String> basicList = new ArrayList<String>();
    //advancedList to store the resources set to be protected in advanced interface
    ArrayList<String> advancedList = new ArrayList<String>();
    
    //advancedSinkList to store the sink to be checked in advanced interface
    final ArrayList<String> advancedSinkList = new ArrayList<String>();
    
    //Add sinkItem is Advanced Interface here
    final CharSequence[] sinkItems = {"Internet", "Sms"};
    
    private boolean[] mulchoice = new boolean[sinkItems.length];
    
    Boolean transCheck = false;
    int timeInterval = 10;
    
    final Context context = this;
    
    //private PopupWindow popupWindow;
    
    SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.monitor = new MonitorAPI(this.getApplicationContext());
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		for(int i=0; i<sinkItems.length; i++){
			advancedSinkList.add(sinkItems[i].toString().toLowerCase());
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
    
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			
			Fragment fragment = null;
			
			switch (position)
			{
				case 0:
					fragment = new BasicFragment();
					bif = (BasicFragment)fragment;
					break;
				case 1:
					fragment = new AdvancedFragment();
					aif = (AdvancedFragment)fragment;
					break;
			}
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 2 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			}
			return null;
		}
	}
   
	//for switch buttons
	public void toggle(final String target, ArrayList<String> toggledList, final String buttonType){
		//get the toggled button
		String switchbutton = target.concat("_switch");
		int id = getResources().getIdentifier(switchbutton, "id", "ntu.fyp.sjy.securitycontroller");
		final Switch switchBtn = (Switch) findViewById(id);
		
		//get the corresponding gridview of the button
		String gridview = target.concat("_grid");
		int idView = getResources().getIdentifier(gridview, "id", "ntu.fyp.sjy.securitycontroller");
    	final MyGridView gridView = (MyGridView) findViewById(idView);
    	
    	//updatePolicy
    	if(switchBtn.isChecked()){
    		if(!toggledList.contains(target)){
				toggledList.add(target);
				setContent(buttonType, toggledList);
			}
			updatePolicy();
			gridView.setVisibility(View.VISIBLE);

			final ArrayList<String> storedValue = getContent(target);
			
			//get all installed applications
	        PackageManager pm = getPackageManager();
	        
	    	List<ApplicationInfo> apps = pm.getInstalledApplications(0);
	    	
	    	
			//setting for the adapter
	        final ArrayList <HashMap<String, Object>> appList = new ArrayList<HashMap<String, Object>>();
	        for(int j=0; j<apps.size(); j++){
	        	apps.get(j);
				if((apps.get(j).flags & ApplicationInfo.FLAG_SYSTEM) == 0){
	        		HashMap<String, Object> map = new HashMap<String, Object>();
		        	map.put("icon", apps.get(j).loadIcon(pm));
		        	map.put("appname", pm.getApplicationLabel(apps.get(j)).toString());
		        	map.put("packageName", apps.get(j).packageName);
		        	map.put("isSelected", storedValue.contains(apps.get(j).packageName));
		        	map.put("UID", apps.get(j).uid);
		        	map.put("source_tableRow_internet", false);
		        	map.put("source_tableRow_file", false);
		        	appList.add(map);
	        	}
	        }
	        
	        SimpleAdapter simpleAdapter = null;
	        
	        if(buttonType.equals("basicList")){
	        	simpleAdapter = new SimpleAdapter(MainActivity.this, appList, R.layout.basic_grid, 
						new String[]{"icon", "appname"}, new int[] {R.id.image, R.id.app_title}){
					@Override
					public View getView(final int position, View convertView, ViewGroup parent){
						View view = super.getView(position, convertView, parent);
						@SuppressWarnings("unchecked")
						final HashMap<String, Object> map = (HashMap<String, Object>) this.getItem(position);
						
							CheckBox checkBox = (CheckBox)view.findViewById(R.id.app_check);
							checkBox.setChecked((Boolean) map.get("isSelected"));
		
							checkBox.setOnClickListener(new View.OnClickListener() {
								
								@Override
								public void onClick(View v) {
									// TODO Auto-generated method stub
									map.put("isSelected", ((CheckBox)v).isChecked());
									if(((CheckBox)v).isChecked()){
										if(!storedValue.contains(map.get("packageName"))){
											storedValue.add((String) map.get("packageName"));
										}
									}else{
										for(int j=0; j<storedValue.size(); j++){
											if(storedValue.get(j).equals(map.get("packageName"))){
												storedValue.remove(j);
											}
										}
									}
									setContent(target, storedValue);
									//update 'trustGroup'
							    	String permission = "trusted" + target.substring(0, 1).toUpperCase() +target.substring(1);
									ArrayList<Integer> UID = new ArrayList<Integer>();
							   		UID.add((Integer) map.get("UID"));
							   		Event ev = new Event(permission, UID, 1);
							   		MainActivity.this.renewMonitorVariable(ev, (Boolean) map.get("isSelected"));
									Toast.makeText(MainActivity.this, "Trusted configuration saved", Toast.LENGTH_LONG).show();
								}
							});
						return view;
					}
				};
				
	        }else if(buttonType.equals("advancedList")){
	        	final String sourceName = target.split("_")[0];
	        	simpleAdapter = new SimpleAdapter(MainActivity.this, appList, R.layout.advanced_grid, 
						new String[]{"icon", "appname"}, new int[] {R.id.sink_app_image, R.id.sink_app_title}){
					@Override
					public View getView(final int position, View convertView, ViewGroup parent){
						final View view = super.getView(position, convertView, parent);
						@SuppressWarnings("unchecked")
						final HashMap<String, Object> map = (HashMap<String, Object>) this.getItem(position);
						
						TextView packageName = (TextView)view.findViewById(R.id.sink_app_title);
						
						packageName.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								//pop up window
								
								AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
								builder.setTitle("Check to forbid access to resource after "+sourceName);
								builder.setMultiChoiceItems(sinkItems, mulchoice, new DialogInterface.OnMultiChoiceClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which, boolean isChecked) {
										// TODO Auto-generated method stub
										for(int i=0; i<sinkItems.length; i++){
											ArrayList<String> sinkStoredValue = getContent(sourceName + "_"+ sinkItems[i].toString().toLowerCase());
											if(mulchoice[i]){
												if(!sinkStoredValue.contains(map.get("packageName"))){
													sinkStoredValue.add((String) map.get("packageName"));
												}
											}else{
												for(int j=0; j<sinkStoredValue.size(); j++){
													if(sinkStoredValue.get(j).equals(map.get("packageName"))){
														sinkStoredValue.remove(j);
													}
												}
											}
											ArrayList<Integer> UID = new ArrayList<Integer>();
									   		UID.add((Integer) map.get("UID"));
									   		Event ev = new Event("trusted_"+ sourceName +"_"+sinkItems[i].toString().toLowerCase(), UID, 1);
									   		MainActivity.this.renewMonitorVariable(ev, !mulchoice[i]);
											Toast.makeText(MainActivity.this, "Trusted configuration saved", Toast.LENGTH_LONG).show();
										}
									}
								});
								
								builder.setPositiveButton("Confirm",null);
								AlertDialog dialog = builder.create();
								dialog.show();
								}
							});
						return view;
					}
				};
	        	
	        }
			
	        gridView.setAdapter(simpleAdapter);
	        
	        //this paragraph of code is used to make the icon display normally
	        simpleAdapter.setViewBinder(new ViewBinder(){
	        	public boolean setViewValue (View view, Object data, String textRepresentation){
	        		if(view instanceof ImageView && data instanceof Drawable){
	        			ImageView iv = (ImageView) view;
	        			iv.setImageDrawable((Drawable)data);
	        			return true;
	        		}
	        		else return false;
	        }
	        });
	        
		}	
		else{ //if the toggle button is disabled
			for(int j=0; j<toggledList.size(); j++){
				if(toggledList.get(j).equals(target)){
					toggledList.remove(j);
					setContent(buttonType, toggledList);
					break;
				}
			}
			updatePolicy();
			
			gridView.setVisibility(View.GONE);
		}
    }
   
    public void internetToggle(View v){
		toggle("internet", basicList, "basicList");
	}
	
	public void contactToggle(View v){
		toggle("contact", basicList, "basicList");
	}
	
	public void locationToggle(View v){
		toggle("location", basicList, "basicList");
	}
	
	public void imeiToggle(View v){
		toggle("imei", basicList, "basicList");
	}
	
	public void smsToggle(View v){
		toggle("sms", basicList, "basicList");
	}
	
	public void callToggle(View v){
		toggle("call_privileged", basicList, "basicList");
	}
	
	public void contactSourceToggle(View v){
		toggle("contact_source", advancedList, "advancedList");
	}
	
	public void locationSourceToggle(View v){
		toggle("location_source", advancedList, "advancedList");
	}
	
	public void smsSourceToggle(View v){
		toggle("sms_source", advancedList, "advancedList");
	}
	
	public void imeiSourceToggle(View v){
		toggle("imei_source", advancedList, "advancedList");
	}
	
	public void timeSubmit(View v){
		EditText time = (EditText) this.findViewById(R.id.time_interval_input);
		timeInterval = Integer.parseInt(time.getText().toString());
	}
	
	public void transferableCheck(View v){
		CheckBox transChk = (CheckBox) findViewById(R.id.transferable_check);
		if(transChk.isChecked()){
			transCheck = true;
		}else{
			transCheck = false;
		}
		updatePolicy();
	}
    
	public int updatePolicy(){
		
		final EditText timeInput = (EditText) this.findViewById(R.id.time_interval_input);
		timeInterval = Integer.parseInt(timeInput.getText().toString());
		
		//update the policy
		try{
			StringBuilder policy = new StringBuilder();
			String policyTemp = PolicyGenerator.policyGenerator(basicList, advancedList, advancedSinkList, transCheck, timeInterval);
			if(policyTemp.equals("")){
				monitor.unregisterMonitor();
				return -1;
			}
			policy.append(policyTemp);
			FileOutputStream fos = openFileOutput("policy", Context.MODE_WORLD_READABLE);
			fos.write(policy.toString().getBytes());
		}catch(Exception ex){Toast.makeText(MainActivity.this, ex.toString(),Toast.LENGTH_LONG).show();}
		
		(new GetPolicyFileTask()).execute("policy");
		   	
		return 0;
    }
    
    public ArrayList<String> getContent(String fileName){
    	String storedValue = "";
    	try
		{
			int buffer; StringBuilder sb = new StringBuilder();
			FileInputStream fis = MainActivity.this.openFileInput(fileName);
			while((buffer = fis.read()) != -1)
			{
				sb.append(Character.toString((char)buffer));
			}
			storedValue = sb.toString();
			fis.close();
		}
		catch(Exception ex)
		{
			Toast.makeText(MainActivity.this, "No existing configuration", Toast.LENGTH_SHORT).show();
		}
    	ArrayList<String> returnValue = new ArrayList<String>();
    	
    	if(!storedValue.equals("")){
    		String[] temp = storedValue.split(";");
    		for(int i=0; i<temp.length; i++){
    			returnValue.add(temp[i]);
    		}
    	}
    	return returnValue;
    }
    
    public void setContent(String fileName, ArrayList<String> list){
    	try{
    		StringBuilder content = new StringBuilder();
    		for(int i=0; i<list.size(); i++){
    			content.append(list.get(i)+";");
    		}
			FileOutputStream fos = openFileOutput(fileName, Context.MODE_WORLD_READABLE);
			fos.write(content.toString().getBytes());
			fos.close();
		}catch(Exception ex){Toast.makeText(MainActivity.this, ex.toString(), Toast.LENGTH_LONG).show();}
    }
    
    
	class GetPolicyFileTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			try{
				GenerateMonitor.generateMonitor(params[0], 2, MainActivity.this);
				return true;
			}catch (Exception e){
				e.printStackTrace();
			}
			return false;
		}
		
		protected void onPostExecute(Boolean result) {
			if(result) {
				monitor.registerMonitor(Print.inputString, Print.relations, Print.policyID, Print.callRelationID);
				System.out.println(Print.formulaPrintOut);
				Toast.makeText(getApplicationContext(), "Policy loaded", Toast.LENGTH_SHORT).show();
			}
			else		
				Toast.makeText(getApplicationContext(), "Unable to load policy file", Toast.LENGTH_SHORT).show();
			
			//get all installed applications
	        PackageManager pm = getPackageManager();
	    	List<ApplicationInfo> apps = pm.getInstalledApplications(0); 
	    	
	    	try{
		    	//set all system applications
		    	for(int j=0; j<apps.size(); j++){
		    		if((apps.get(j).flags & ApplicationInfo.FLAG_SYSTEM) != 0 || apps.get(j).packageName.equals("system")){
		    		//if(systemApps.contains(apps.get(j).packageName)){	
		    			ArrayList<Integer> UID = new ArrayList<Integer>();
				    	UID.add((Integer) apps.get(j).uid);
				    	Event ev = new Event("system", UID, 1);
				    	MainActivity.this.renewMonitorVariable(ev, true);
		    		}
		    	}
		    	
		    	//set all basic interface related attributes
		    	for(int j=0; j<basicList.size(); j++){
		    		ArrayList<String> tempArray = getContent(basicList.get(j));
		    		for(int k=0; k<tempArray.size(); k++){
		    			ArrayList<Integer> UID = new ArrayList<Integer>();
						try {
							UID.add(pm.getApplicationInfo(tempArray.get(k), PackageManager.GET_ACTIVITIES).uid);
						} catch (NameNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				    	Event ev = new Event("trusted"+basicList.get(j).replaceFirst(basicList.get(j).substring(0, 1), basicList.get(j).substring(0, 1).toUpperCase()), UID, 1);
				    	MainActivity.this.renewMonitorVariable(ev, true);
		    		} 
		    	}
		    	
		    	//set all advanced interface related attributes
		    	for(int j=0; j<advancedList.size(); j++){
		    		for(int i=0;i<advancedSinkList.size(); i++){
			    		ArrayList<String> tempArray = getContent(advancedList.get(j).split("_")[0]+advancedSinkList.get(i));
			        	for(int k=0; k<apps.size(); k++){
			        		ArrayList<Integer> UID = new ArrayList<Integer>();
			    		    UID.add((Integer) apps.get(k).uid);
			    		    Event ev;
			    		    if(tempArray.contains(apps.get(k).packageName))
			    		    	ev = new Event("trusted_"+ advancedList.get(j).split("_")[0] +"_" +advancedSinkList.get(i), UID, 0);
			    		    else
			    		    	ev = new Event("trusted_"+ advancedList.get(j).split("_")[0] +"_" +advancedSinkList.get(i), UID, 1);;
			    		    MainActivity.this.renewMonitorVariable(ev, true);
			        	}
		    		}
		    	}}catch(Exception e){
		    		e.printStackTrace();
		    	}
		}
	}
}