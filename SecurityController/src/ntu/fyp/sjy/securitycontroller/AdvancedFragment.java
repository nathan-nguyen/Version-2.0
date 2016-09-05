package ntu.fyp.sjy.securitycontroller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AdvancedFragment extends Fragment {
	private View rootView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
			rootView = inflater.inflate(R.layout.advanced_interface, container,
				false);
		
		final MainActivity temp = ((MainActivity) this.getActivity());
		
		//set the toggle listener for each switch button
		
		//checked listener for contact
		Switch switchBtnContact = (Switch) rootView.findViewById(R.id.contact_source_switch);
		switchBtnContact.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				temp.contactSourceToggle(rootView);
			}
			
		});
		
		//checked listener for location
		Switch switchBtnLocation = (Switch) rootView.findViewById(R.id.location_source_switch);
		switchBtnLocation.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				temp.locationSourceToggle(rootView);
			}
			
		});
		
		//checked listener for IMEI
		Switch switchBtnImei = (Switch) rootView.findViewById(R.id.imei_source_switch);
		switchBtnImei.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				temp.imeiSourceToggle(rootView);
			}
			
		});
		
		//checked listener for SMS
		Switch switchBtnSms = (Switch) rootView.findViewById(R.id.sms_source_switch);
		switchBtnSms.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				temp.smsSourceToggle(rootView);
			}
			
		});
		
		return rootView;
	}
	
	@Override
	public void onStart(){
		super.onStart();

		MainActivity temp = ((MainActivity) this.getActivity());
		temp.advancedList = temp.getContent("advancedList");
		for(int i=0; i<temp.advancedList.size(); i++){
			int id = getResources().getIdentifier(temp.advancedList.get(i).concat("_switch"), "id", "ntu.fyp.sjy.securitycontroller");
			System.out.println(temp.advancedList.get(i).concat("_switch"));
			final Switch switchBtn = (Switch) getActivity().findViewById(id);
			switchBtn.setChecked(false);
			switchBtn.performClick();
		}
	}
}
