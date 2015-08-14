//-----------------------------------------------------------------------------
//
// (C) Rob Beane, 2015 <robbeane@gmail.com>
//
//-----------------------------------------------------------------------------

package org.axdev.cpuspy.activity;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import org.axdev.cpuspy.R;
import org.axdev.cpuspy.fragments.BackHandledFragment;
import org.axdev.cpuspy.fragments.InfoFragment;
import org.axdev.cpuspy.fragments.TimerFragment;
import org.axdev.cpuspy.services.SleepService;
import org.axdev.cpuspy.utils.ThemeUtils;
import org.axdev.cpuspy.utils.TypefaceHelper;
import org.axdev.cpuspy.utils.TypefaceSpan;
import org.axdev.cpuspy.utils.Utils;
import org.axdev.cpuspy.widget.SlidingTabLayout;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements BackHandledFragment.BackHandlerInterface {

    private boolean mLastTheme;
    private boolean mLastNavBar;
    private BackHandledFragment selectedFragment;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ThemeUtils.onActivityCreateSetNavBar(this);
        }
        ThemeUtils.onActivityCreateSetTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        setupTabs();

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        final ActionBar mActionBar = getSupportActionBar();
        assert mActionBar != null;
        /** Use custom Typeface for action bar title on KitKat devices */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActionBar.setTitle(getResources().getString(R.string.app_name_long));
            mActionBar.setElevation(0);
        } else {
            final SpannableString s = new SpannableString(getResources().getString(R.string.app_name_long));
            s.setSpan(new TypefaceSpan(this, TypefaceHelper.MEDIUM_FONT), 0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Update the action bar title with the TypefaceSpan instance
            mActionBar.setTitle(s);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mLastTheme = ThemeUtils.darkTheme;
        mLastNavBar = ThemeUtils.coloredNavBar;

        // Show warning dialog if Xposed is installed
        if (Utils.isXposedInstalled(this)) {
            final boolean showXposedWarning = sp.getBoolean("showXposedWarning", true);

            if (showXposedWarning) {
                final MaterialDialog dialog = new MaterialDialog.Builder(this)
                        .title(getResources().getString(R.string.xposed_warning_title))
                        .content(getResources().getString(R.string.xposed_warning_content))
                        .positiveText(getResources().getString(R.string.action_dismiss))
                        .positiveColor(getResources().getColor(R.color.primary))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                dialog.dismiss();
                            }
                        })
                        .dismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                sp.edit().putBoolean("crashReport", false).apply();
                                sp.edit().putBoolean("showXposedWarning", false).apply();
                            }
                        })
                        .build();

                // Override dialog enter/exit animation
                dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
                dialog.show();
            }
        }

        // This must always be updated to reflect the upcoming version
        final String upcomingVersionURL = "https://app.box.com/cpuspy-v317";

        // Check if an update is available
        final Thread t = new Thread(new Runnable() {
            public void run() {
                if (Utils.urlExists(upcomingVersionURL)) {
                    SnackbarManager.show(
                            Snackbar.with(MainActivity.this)
                                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                                    .text(getResources().getString(R.string.snackbar_text_update))
                                    .actionLabel(getResources().getString(R.string.action_view))
                                    .actionLabelTypeface(mediumTypeface())
                                    .actionColor(getResources().getColor(R.color.primary))
                                    .actionListener(new ActionClickListener() {
                                        @Override
                                        public void onActionClicked(Snackbar snackbar) {
                                            final String xdaURL = "http://goo.gl/AusQy8";
                                            try {
                                                final Intent i = new Intent(Intent.ACTION_VIEW);
                                                i.setData(Uri.parse(xdaURL));
                                                startActivity(i);
                                            } catch (ActivityNotFoundException e) {
                                                Log.e("CpuSpy", "Error opening: " + xdaURL);
                                            }
                                        }
                                    })
                    );
                }
            }
        });
        t.start();

        // Start service if its not automatically started on boot
        if (sp.getBoolean("sleepDetection", true)) {
            if (!Utils.isServiceRunning(this, SleepService.class)) {
                startService(new Intent(this, SleepService.class));
            }
        }
    }

    private void setupTabs() {
        // Assigning ViewPager View and setting the adapter
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        if (viewPager != null) {
            setupViewPager(viewPager);
        }

        // Assiging the Sliding Tab Layout View
        final SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setCustomTabView(R.layout.tab_layout, 0);
        tabs.setDistributeEvenly(true); // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width

        // Setting Custom Color for the Scroll bar indicator of the Tab View
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.tabsScrollColor);
            }
        });

        tabs.setViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        final Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(new TimerFragment());
        adapter.addFragment(new InfoFragment());
        viewPager.setAdapter(adapter);
    }

    public class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment) {
            mFragments.add(fragment);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        private final int[] imageResId = {
                R.drawable.ic_tab_timers,
                R.drawable.ic_tab_info
        };

        @Override
        public CharSequence getPageTitle(int position) {
            final Drawable image = ResourcesCompat.getDrawable(getResources(), imageResId[position], null);
            assert image != null;
            image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            final SpannableString sb = new SpannableString(" ");
            final ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
            sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        }
    }

    @Override
    public void onBackPressed() {
        if(selectedFragment == null || !selectedFragment.onBackPressed()) {
            // Selected fragment did not consume the back press event.
            super.onBackPressed();
        }
    }

    @Override
    public void setSelectedFragment(BackHandledFragment selectedFragment) {
        this.selectedFragment = selectedFragment;
    }

    private Typeface mediumTypeface() {
        Typeface mediumFont;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediumFont = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } else {
            mediumFont = TypefaceHelper.get(this, TypefaceHelper.MEDIUM_FONT);
        }

        return mediumFont;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize and start automatic crash reporting
        if (sp.getBoolean("crashReport", true) && !Utils.isXposedInstalled(this)) {
            Fabric.with(this, new Crashlytics());
        } else {
            sp.edit().putBoolean("crashReport", false).apply();
        }

        // Restart activity if theme or navbar changed
        if (mLastTheme != ThemeUtils.darkTheme
                || mLastNavBar != ThemeUtils.coloredNavBar) {
            this.recreate();
        }
    }
}