//-----------------------------------------------------------------------------
//
// (C) Rob Beane, 2015 <robbeane@gmail.com>
//
//-----------------------------------------------------------------------------

package org.axdev.cpuspy.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.app.ListFragment;
import android.widget.TextView;

import org.axdev.cpuspy.R;
import org.axdev.cpuspy.utils.TypefaceHelper;
import org.axdev.cpuspy.utils.TypefaceSpan;

public class LicenseFragment extends ListFragment implements AdapterView.OnItemClickListener {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.license_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.licenses, android.R.layout.simple_list_item_1);

        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);

        final ActionBar mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert mActionBar != null;
        mActionBar.setDisplayHomeAsUpEnabled(true);

        /** Use custom Typeface for action bar title on KitKat devices */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActionBar.setTitle(R.string.pref_title_license);
        } else {
            final SpannableString s = new SpannableString(getResources().getString(R.string.pref_title_license));
            s.setSpan(new TypefaceSpan(getActivity(), TypefaceHelper.MEDIUM_FONT), 0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Update the action bar title with the TypefaceSpan instance
            mActionBar.setTitle(s);
        }

        TextView mLicenseHeader = (TextView) getActivity().findViewById(R.id.license_header);
        setMediumTypeface(mLicenseHeader);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                openURL("http://developer.android.com/tools/support-library/index.html");
                break;
            case 1:
                openURL("https://github.com/JakeWharton/butterknife");
                break;
            case 2:
                openURL("https://github.com/afollestad/material-dialogs");
                break;
            case 3:
                openURL("https://github.com/balysv/material-ripple");
                break;
            case 4:
                openURL("https://github.com/MrBIMC/MaterialSeekBarPreference");
                break;
            case 5:
                openURL("https://github.com/nispok/snackbar");
                break;
        }
    }

    private void openURL(String s) {
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(s));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("CpuSpy", "Error opening: " + s);
        }
    }

    /** Apply Roboto-Medium typeface */
    private void setMediumTypeface(TextView tv) {
        Typeface mediumFont;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediumFont = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } else {
            mediumFont = TypefaceHelper.get(getActivity(), TypefaceHelper.MEDIUM_FONT);
        }

        tv.setTypeface(mediumFont);
    }
}