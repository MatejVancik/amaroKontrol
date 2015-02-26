package com.mv2studio.amarok.kontrol.ui.Fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.mv2studio.amarok.kontrol.ui.Activity.SingleContentFragmentActivity;

public class BaseFragment extends Fragment implements SingleContentFragmentActivity.OnBackPressedListener {

    private Bundle mBundle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRetainInstance(true);
    }

    @Override
    public void onBack() {
        // do not force fragments to implement this
    }

    public void setBundle(Bundle bundle) {
        mBundle = bundle;
    }

    protected Bundle getBundle() {
        return mBundle;
    }

}
