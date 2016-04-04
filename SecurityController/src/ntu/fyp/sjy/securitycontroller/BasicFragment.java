package ntu.fyp.sjy.securitycontroller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.Switch;

	public class BasicFragment extends Fragment{
		private View rootView;
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
				rootView = inflater.inflate(R.layout.basic_interface, container,
					false);			
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