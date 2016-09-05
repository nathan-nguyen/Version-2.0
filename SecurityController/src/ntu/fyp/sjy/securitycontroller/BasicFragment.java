package ntu.fyp.sjy.securitycontroller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;

	public class BasicFragment extends Fragment{
		private View rootView;
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
				rootView = inflater.inflate(R.layout.basic_interface, container,
					false);	
				
				final MainActivity temp = ((MainActivity) this.getActivity());
				
				//Set the toggle listener for each switch button
				
				//checked listener for internet 
				Switch switchBtnInternet = (Switch) rootView.findViewById(R.id.internet_switch);
				switchBtnInternet.setOnCheckedChangeListener(new OnCheckedChangeListener(){

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						temp.internetToggle(rootView);
					}
					
				});
				
				//checked listener for contact
				Switch switchBtnContact = (Switch) rootView.findViewById(R.id.contact_switch);
				switchBtnContact.setOnCheckedChangeListener(new OnCheckedChangeListener(){

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						temp.contactToggle(rootView);
					}
					
				});
				
				//checked listener for location
				Switch switchBtnLocation = (Switch) rootView.findViewById(R.id.location_switch);
				switchBtnLocation.setOnCheckedChangeListener(new OnCheckedChangeListener(){

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						temp.locationToggle(rootView);
					}
					
				});
				
				//checked listener for IMEI
				Switch switchBtnImei = (Switch) rootView.findViewById(R.id.imei_switch);
				switchBtnImei.setOnCheckedChangeListener(new OnCheckedChangeListener(){

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						temp.imeiToggle(rootView);
					}
					
				});
				
				//checked listener for SMS
				Switch switchBtnSms = (Switch) rootView.findViewById(R.id.sms_switch);
				switchBtnSms.setOnCheckedChangeListener(new OnCheckedChangeListener(){

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						temp.smsToggle(rootView);
					}
					
				});
				
				//checked listener for Call
				Switch switchBtnCall = (Switch) rootView.findViewById(R.id.call_privileged_switch);
				switchBtnCall.setOnCheckedChangeListener(new OnCheckedChangeListener(){

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						temp.callToggle(rootView);
					}
					
				});
			return rootView;
		}
		
		
		public void onStart(){
			super.onStart();
			
			final MainActivity temp = ((MainActivity) this.getActivity());
			temp.basicList = temp.getContent("basicList");
			for(int i=0; i<temp.basicList.size(); i++){
				int id = getResources().getIdentifier(temp.basicList.get(i).concat("_switch"), "id", "ntu.fyp.sjy.securitycontroller");
				final Switch switchBtn = (Switch) getActivity().findViewById(id);
				switchBtn.setChecked(false);
				switchBtn.performClick();
			}
		}
	}