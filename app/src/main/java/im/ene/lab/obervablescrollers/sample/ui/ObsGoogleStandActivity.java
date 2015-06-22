package im.ene.lab.obervablescrollers.sample.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;

import butterknife.ButterKnife;
import butterknife.InjectView;
import im.ene.lab.obervablescrollers.sample.R;
import im.ene.lab.obervablescrollers.sample.fragment.DummyRecyclerViewFragment;
import im.ene.lab.obervablescrollers.sample.fragment.DummyScrollViewFragment;
import im.ene.lab.obervablescrollers.sample.util.UIUtil;
import im.ene.lab.obervablescrollers.sample.widget.AlphaForegroundColorSpan;
import im.ene.lab.observablescrollers.lib.adapter.SmartFragmentStatePagerAdapter;
import im.ene.lab.observablescrollers.lib.fragment.ObsFragment;
import im.ene.lab.observablescrollers.lib.util.LogHelper;
import im.ene.lab.observablescrollers.lib.util.OnScrollObservedListener;
import im.ene.lab.observablescrollers.lib.util.Scrollable;

public class ObsGoogleStandActivity extends BaseActivity implements OnScrollObservedListener {

    @InjectView(R.id.tabs)
    PagerSlidingTabStrip mTabs;
    @InjectView(R.id.viewpager)
    ViewPager mViewPager;
    @InjectView(R.id.main_header)
    View mMainHeader;
    @InjectView(R.id.pager_header)
    View mPagerHeader;
    @InjectView(R.id.header_thumbnail)
    View mThumbNail;

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mSpannableString;

    private String[] mTitles;

    private int mMainHeaderHeight;
    private int mPagerHeaderHeight;
    private int mActionbarHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obs_google_stand);

        mMainHeaderHeight = getResources().getDimensionPixelSize(R.dimen.google_play_header_large);
        mPagerHeaderHeight = getResources().getDimensionPixelSize(R.dimen.google_play_header_middle);
        mActionbarHeight = UIUtil.getActionbarToolbarHeight(this);

        mSpannableString = new SpannableString(getString(R.string.title_activity_obs_google_stand));
        getTheme().resolveAttribute(R.attr.colorPrimary, mTypedValue, true);
        toolbarColor = new ColorDrawable(mTypedValue.data);
    }

    private int mCurrentScrollY;

    private void setTitleAlpha(float alpha) {
        if (mSpannableString == null || mAlphaForegroundColorSpan == null)
            return;
        mAlphaForegroundColorSpan.setAlpha(alpha);
        mSpannableString.setSpan(mAlphaForegroundColorSpan, 0,
                mSpannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        setTitle(mSpannableString);
    }

    private final TypedValue mTypedValue = new TypedValue();
    private ColorDrawable toolbarColor;

    private void setToolbarAlpha(float alpha) {
        toolbarColor.setAlpha((int) (alpha * 255));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(toolbarColor);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ButterKnife.inject(this);
        mTitles = getResources().getStringArray(R.array.item_titles);
        final ViewPagerAdapter mAdapter = new ViewPagerAdapter(getSupportFragmentManager(), mTitles);
        mViewPager.setAdapter(mAdapter);
        mTabs.setViewPager(mViewPager);
        mTabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            private int currentItemPosition = 0;

            private int expectedNextPosition = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (currentItemPosition == position) {
                    if (positionOffset < 0.5) {
                        expectedNextPosition = position + 1;
                    }
                } else {
                    if (positionOffset > 0.5) {
                        expectedNextPosition = position;
                    }
                }

                // I would like to scroll the "adjacent page" here to prevent them from
                // visible during the scrolling.
                // in ObsRecyclerView#getVerticalScrollOffset() I disallow the scroll by checking the
                // vertical scroll value so there is no need to worry about to many method calls.
                // I'm aware of the trade off when asking it to call many time. Please give me a better call here
                Fragment expectedNextItem = mAdapter.getRegisteredFragment(expectedNextPosition);
                if (expectedNextItem != null && expectedNextItem instanceof ObsFragment) {
                    Scrollable currentScrollable = ((ObsFragment) expectedNextItem).getScrollable();
                    currentScrollable.scrollVerticallyBy(mCurrentScrollY -
                            currentScrollable.getVerticalScrollOffset());
                }
            }

            @Override
            public void onPageSelected(int position) {
                currentItemPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

        });

    }

    @Override
    public void onScrollChanged(Scrollable scroller, int dx, int dy) {
        float scrollY = scroller.getVerticalScrollOffset();

        mCurrentScrollY = (int) scrollY;
        float parallax = Math.min(0, Math.max(-mMainHeaderHeight, -scrollY / 2));
        ViewCompat.setTranslationY(mMainHeader, parallax);

        float transition = Math.min(0, Math.max(-mPagerHeaderHeight, -scrollY));
        ViewCompat.setTranslationY(mPagerHeader, transition);

        float alpha = Math.min(1, Math.max(0, (scrollY * 2) / mPagerHeaderHeight));
        LogHelper.d("scrollY", scrollY + "");
        ViewCompat.setAlpha(mThumbNail, 1.0f - alpha);

        if ((mPagerHeaderHeight - scrollY) <= UIUtil.getActionbarToolbarHeight(this)) {
            UIUtil.setPaddingAnimation(mTabs, mTabs.getTabPaddingLeftRight(), mTabs.getTabPaddingLeftRight());
            mTabs.setPaddingMiddle(false);
        }

    }

    @Override
    public void onScrollStateChanged(Scrollable scroller, Scrollable.ScrollState newState) {

    }

    private class ViewPagerAdapter extends SmartFragmentStatePagerAdapter {

        private String[] titles;

        public ViewPagerAdapter(FragmentManager fm, String[] titles) {
            super(fm);
            this.titles = titles;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            if (position % 3 == 0)
                fragment = DummyRecyclerViewFragment.newInstance();
            else
                fragment = DummyScrollViewFragment.newInstance();
            return fragment;
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }
}
