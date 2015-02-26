package com.mv2studio.amarok.kontrol.ui.Activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.ui.Fragment.BaseFragment;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class SingleContentFragmentActivity extends FragmentActivity {

    private OnBackPressedListener mBackPressedListener;

    public interface OnBackPressedListener {
        public void onBack();
    }

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        mBackPressedListener = listener;
    }

    @Override
    public void onBackPressed() {
        // show previous fragment if there's some
        showPreviousFragment();

        if (mBackPressedListener != null) {
            mBackPressedListener.onBack();
        }

        // restore onBackPressedListener
        try {
            setOnBackPressedListener(getTopFragment());
        } catch (NoSuchElementException e) { e.printStackTrace(); }

        // finish activity if no fragment in backstack
        if (fragmentBackStack.isEmpty()) {
            finish();
        }
    }

    LinkedList<BaseFragment> fragmentBackStack = new LinkedList<BaseFragment>();

    /**
     * Replaces fragment on top of fragment content view
     *
     * @param fragment       fragment to show
     * @param clearBackStack clear fragment back stack
     */
    public void replaceFragment(BaseFragment fragment, boolean clearBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();

        setOnBackPressedListener(fragment);

        try {
            BaseFragment top = fragmentBackStack.getLast();
            transaction
                    .hide(top);
        } catch (NoSuchElementException e) {
            // no element in stack
        }

        // clear backstack if needed
        if (clearBackStack) {
            for (Fragment f : fragmentBackStack) {
                transaction.remove(f);
            }
            fragmentBackStack.clear();
        }

        transaction
                .add(R.id.content_frame, fragment);
        fragmentBackStack.add(fragment);

        transaction.commit();
    }

    public BaseFragment getTopFragment() {
        return fragmentBackStack.getLast();
    }

    public void showPreviousFragment() {
        try {
            // fragment on top - remove this fragment
            BaseFragment top = fragmentBackStack.getLast();
            fragmentBackStack.removeLast();

            // second fragment, show this one
            BaseFragment toShow = fragmentBackStack.getLast();

            getSupportFragmentManager().beginTransaction()
                    .remove(top)
                    .show(toShow)
                    .commit();

            // restore actionbar title if possible
//            toShow.onRestoreActionBar();

        } catch (NoSuchElementException e) {
            // no element in stack
        }
    }

    public void showBottomFragment() {
        try {
            BaseFragment top = fragmentBackStack.getLast();

            BaseFragment bottom = fragmentBackStack.getFirst();

            if(top == bottom) return;

            getSupportFragmentManager().beginTransaction()
                    .remove(top)
                    .show(bottom)
                    .commit();

            // remove all
            fragmentBackStack.clear();

            // pub first back
            fragmentBackStack.add(bottom);

        } catch (NoSuchElementException e) {
            // no element in stack
        }
    }

    /**
     * Returns number of backstack entries.
     * @return
     */
    public int getBackStackEntryCount() {
        return fragmentBackStack.size() - 1;
    }

    public void clearFragmentBackStack() {
        fragmentBackStack.clear();
    }

}

