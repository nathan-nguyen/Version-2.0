package ntu.fyp.sjy.securitycontroller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class AdvancedFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.advanced_interface, container,
				false);
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
