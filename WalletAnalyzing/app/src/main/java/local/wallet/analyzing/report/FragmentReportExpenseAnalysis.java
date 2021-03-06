package local.wallet.analyzing.report;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import local.wallet.analyzing.R;
import local.wallet.analyzing.utils.LogUtils;
import local.wallet.analyzing.main.ActivityMain;
import local.wallet.analyzing.main.Configs;
import local.wallet.analyzing.model.Account;
import local.wallet.analyzing.model.Category;
import local.wallet.analyzing.model.Transaction;
import local.wallet.analyzing.sqlite.helper.DatabaseHelper;

/**
 * Created by huynh.thanh.huan on 2/22/2016.
 */
public class FragmentReportExpenseAnalysis extends Fragment implements View.OnClickListener, FragmentReportExpenseAnalysisCategory.ISelectReportExpenseAnalysisCategory, FragmentReportExpenseAnalysisTime.ISelectReportExpenseAnalysisTime, FragmentReportSelectAccount.ISelectReportAccount {
    public static final int         mTab = 4;
    public static final String      Tag = "---[" + mTab + ".2]---ReportExpenseAnalysis";

    private ActivityMain            mActivity;

    private DatabaseHelper  mDbHelper;
    private Configs mConfigs;

    private int[]           mCategoryId = new int[0];   // All Categories
    private int[]           mAccountId  = new int[0];   // All Accounts
    private int             mViewedBy   = 0;            // 1 is Monthly

    private LinearLayout    llCategories;
    private TextView        tvCategory;
    private LinearLayout    llAccounts;
    private TextView        tvAccount;
    private LinearLayout    llViewedBy;
    private TextView        tvViewedBy;

    private LineChart       mLineChart;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);
        LogUtils.logLeaveFunction(Tag);
        return inflater.inflate(R.layout.layout_fragment_report_expense_analysis, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);
        super.onActivityCreated(savedInstanceState);

        mActivity       = (ActivityMain) getActivity();

        mConfigs        = new Configs(getContext());
        mDbHelper       = new DatabaseHelper(getActivity());

        // Update DateTime
        List<Transaction> arTransactions = mDbHelper.getAllTransactions();
        if(arTransactions.size() > 0) {
            // Update list of selected Account at first time
            List<Account> arAccounts = mDbHelper.getAllAccounts();
            mAccountId  = new int[arAccounts.size()];
            for(int i = 0 ; i < arAccounts.size(); i++) {
                mAccountId[i] = arAccounts.get(i).getId();
            }

            // Update list Category
            List<Category> arCategories = mDbHelper.getAllCategories();
            mCategoryId = new int[arCategories.size()];
            for(int i = 0; i< arCategories.size(); i++) {
                mCategoryId[i] = arCategories.get(i).getId();
            }

            llCategories    = (LinearLayout) getView().findViewById(R.id.llCategories);
            llCategories.setOnClickListener(this);
            tvCategory      = (TextView) getView().findViewById(R.id.tvCategory);
            llAccounts      = (LinearLayout) getView().findViewById(R.id.llAccounts);
            llAccounts.setOnClickListener(this);
            tvAccount       = (TextView) getView().findViewById(R.id.tvAccount);
            llViewedBy      = (LinearLayout) getView().findViewById(R.id.llViewedBy);
            llViewedBy.setOnClickListener(this);
            tvViewedBy      = (TextView) getView().findViewById(R.id.tvViewedBy);
            tvViewedBy.setText(getResources().getStringArray(R.array.report_expense_analysis_ar_viewedby)[mViewedBy]);
            mLineChart      = (LineChart) getView().findViewById(R.id.lineChart);

            updateMultiLineChart();

        } else {
            ((ActivityMain) getActivity()).showError(getResources().getString(R.string.Error_Startup_No_Data));
//            ((ActivityMain) getActivity()).setCurrentVisibleItem(ActivityMain.TAB_POSITION_TRANSACTIONS);
        }

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogUtils.logEnterFunction(Tag);
        super.onCreateOptionsMenu(menu, inflater);

        if(mTab != mActivity.getCurrentVisibleItem()) {
            LogUtils.error(Tag, "Wrong Tab. Return");
            LogUtils.logLeaveFunction(Tag);
            return;
        }

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llCategories: {
                showListCategories();
                break;
            }
            case R.id.llAccounts: {
                showListAccounts();
                break;
            }
            case R.id.llViewedBy: {
                showListTime();
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onReportExpenseAnalysisCategorySelected(int[] categories) {
        LogUtils.logEnterFunction(Tag, "categoryId = " + Arrays.toString(categories));

        mCategoryId = categories;

        if(categories.length == mDbHelper.getAllCategories().size()) {
            tvCategory.setText(getResources().getString(R.string.report_expense_analysis_categories_all_categories));
        } else if(categories.length == (mDbHelper.getAllCategories(true).size())) {
            tvCategory.setText(getResources().getString(R.string.report_expense_analysis_categories_all_expense_categories));
        } else if(categories.length == (mDbHelper.getAllCategories(false).size())) {
            tvCategory.setText(getResources().getString(R.string.report_expense_analysis_categories_all_income_categories));
        } else {
            String category = "";
            for(int i = 0 ; i < mCategoryId.length; i++) {
                if(!category.equals("")) {
                    category += ", ";
                }
                category += mDbHelper.getCategory(mCategoryId[i]).getName();
            }

            tvCategory.setText(category);
        }

        updateMultiLineChart();

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onReportExpenseAnalysisTimeSelected(int time) {
        LogUtils.logEnterFunction(Tag, "time = " + time);

        mViewedBy = time;

        String[] arTimes = getResources().getStringArray(R.array.report_expense_analysis_ar_viewedby);

        tvViewedBy.setText(arTimes[mViewedBy]);

        updateMultiLineChart();

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onReportAccountSelected(int[] accountId) {
        LogUtils.logEnterFunction(Tag, "accountId = " + Arrays.toString(accountId));

        mAccountId = accountId;

        if(mAccountId.length == mDbHelper.getAllAccounts().size()) {
            tvAccount.setText(getResources().getString(R.string.report_evi_accounts_all_accounts));
        } else {
            String account = "";
            for(int i = 0 ; i < mAccountId.length; i++) {
                if(!account.equals("")) {
                    account += ", ";
                }
                account += mDbHelper.getAccount(mAccountId[i]).getName();
            }

            tvAccount.setText(account);
        }

        updateMultiLineChart();

        LogUtils.logLeaveFunction(Tag);
    }

    private void updateMultiLineChart() {
        mLineChart = (LineChart) getView().findViewById(R.id.lineChart);
        mLineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

            }

            @Override
            public void onNothingSelected() {}
        });

        mLineChart.setDrawGridBackground(false);
        mLineChart.setDescription("");
        mLineChart.setDrawBorders(false);

        mLineChart.getAxisLeft().setDrawAxisLine(false);
        mLineChart.getAxisLeft().setDrawGridLines(false);
        mLineChart.getAxisRight().setDrawAxisLine(false);
        mLineChart.getAxisRight().setDrawGridLines(false);
        mLineChart.getXAxis().setDrawAxisLine(false);
        mLineChart.getXAxis().setDrawGridLines(false);

        // enable touch gestures
        mLineChart.setTouchEnabled(true);

        // enable scaling and dragging
        mLineChart.setDragEnabled(true);
        mLineChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mLineChart.setPinchZoom(false);

        Legend l = mLineChart.getLegend();
        l.setPosition(Legend.LegendPosition.ABOVE_CHART_CENTER);

        // add a lot of colors
        ArrayList<Integer> colorsIncome = new ArrayList<Integer>();

        for (int c : ColorTemplate.JOYFUL_COLORS) {
            colorsIncome.add(c);
        }

        for (int c : ColorTemplate.COLORFUL_COLORS) {
            colorsIncome.add(c);
        }

        for (int c : ColorTemplate.LIBERTY_COLORS) {
            colorsIncome.add(c);
        }

        for (int c : ColorTemplate.PASTEL_COLORS) {
            colorsIncome.add(c);
        }

        for (int c : ColorTemplate.VORDIPLOM_COLORS) {
            colorsIncome.add(c);
        }

        boolean isAddIndex = false;
        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        ArrayList<String> xVals = new ArrayList<String>();
        List<Transaction> arAllTransactions = mDbHelper.getAllTransactions();

        for(int i = 0 ; i < mCategoryId.length; i++) {

            if(mDbHelper.getTransactionsByTimeAndCategory(new int[]{mCategoryId[i]}, null, null).size() == 0) {
                continue;
            }

            Calendar startDate  = Calendar.getInstance();
            startDate.setTimeInMillis(arAllTransactions.get(arAllTransactions.size() - 1).getTime().getTimeInMillis());
            switch (mViewedBy) {
                case 0: // Weekly
                    startDate.set(Calendar.DAY_OF_WEEK, startDate.getFirstDayOfWeek());
                    break;
                case 1: // Monthly
                    startDate.set(Calendar.DAY_OF_MONTH, 1);
                    break;
                case 2: // Quarterly
                case 3: // Yearly
                    startDate.set(Calendar.MONTH, 0);
                    startDate.set(Calendar.DATE, 1);
                    startDate.set(Calendar.HOUR_OF_DAY, 0);
                    startDate.clear(Calendar.MINUTE);
                    startDate.clear(Calendar.SECOND);
                    startDate.clear(Calendar.MILLISECOND);
                    break;
                default:
                    break;
            }

            Calendar endDate    = Calendar.getInstance();
            endDate.setTimeInMillis(startDate.getTimeInMillis());

            ArrayList<Entry> values = new ArrayList<Entry>();

            int index = 0;
            do {
                if(!isAddIndex) {
                    switch (mViewedBy) {
                        case 0: // Weekly
                            xVals.add(startDate.get(Calendar.DAY_OF_MONTH) + "/" + (startDate.get(Calendar.MONTH) + 1));
                            break;
                        case 1: // Monthly
                            xVals.add((startDate.get(Calendar.MONTH) + 1) + "/" + startDate.get(Calendar.YEAR));
                            break;
                        case 2: // Quarterly
                            xVals.add((startDate.get(Calendar.MONTH) + 1) + "/" + startDate.get(Calendar.YEAR));
                            break;
                        case 3: // Yearly
                            xVals.add(startDate.get(Calendar.YEAR) + "");
                            break;
                        default:
                            break;
                    }
                }

                // Update End Date
                switch (mViewedBy) {
                    case 0: // Weekly
                        endDate.add(Calendar.WEEK_OF_YEAR, 1);
                        break;
                    case 1: // Monthly
                        endDate.add(Calendar.MONTH, 1);
                        break;
                    case 2: // Quarterly
                        endDate.add(Calendar.MONTH, 3);
                        break;
                    case 3: // Yearly
                        endDate.add(Calendar.YEAR, 1);
                        break;
                    default:
                        break;
                }


                List<Transaction> arTransactions = mDbHelper.getTransactionsByTimeAndCategory(new int[]{mCategoryId[i]}, startDate, endDate);
                double value = 0.0;
                for(Transaction tran : arTransactions) {
                    value += tran.getAmount();
                }
                values.add(new Entry((float) value, index));

                LogUtils.trace(Tag, mDbHelper.getCategory(mCategoryId[i]).getName() + " : " + String.format(getResources().getString(R.string.format_budget_date_2),
                        startDate.get(Calendar.DAY_OF_MONTH),
                        startDate.get(Calendar.MONTH) + 1,
                        endDate.get(Calendar.DAY_OF_MONTH),
                        endDate.get(Calendar.MONTH) + 1) + " : " + value);

                // Update Start Date
                switch (mViewedBy) {
                    case 0: // Weekly
                        startDate.add(Calendar.WEEK_OF_YEAR, 1);
                        break;
                    case 1: // Monthly
                        startDate.add(Calendar.MONTH, 1);
                        break;
                    case 2: // Quarterly
                        startDate.add(Calendar.MONTH, 3);
                        break;
                    case 3: // Yearly
                        startDate.add(Calendar.YEAR, 1);
                        break;
                    default:
                        break;
                }

                index++;
            } while(endDate.getTimeInMillis() <= arAllTransactions.get(0).getTime().getTimeInMillis());

            isAddIndex = true;

            LineDataSet d = new LineDataSet(values, mDbHelper.getCategory(mCategoryId[i]).getName());
            d.setLineWidth(2.5f);
            d.setCircleRadius(4f);

            int color = colorsIncome.get(i % colorsIncome.size());
            d.setColor(color);
            d.setCircleColor(color);
            dataSets.add(d);

        }

        LineData data = new LineData(xVals, dataSets);
        mLineChart.setData(data);

        mLineChart.invalidate();

    }

    /**
     * Start Fragment ReportExpenseAnalysisCategory
     */
    private void showListCategories() {
        LogUtils.logEnterFunction(Tag);
        FragmentReportExpenseAnalysisCategory nextFrag = new FragmentReportExpenseAnalysisCategory();
        Bundle bundle = new Bundle();
        bundle.putInt("Tab", mTab);
        bundle.putString("Fragment", Tag);
        bundle.putIntArray("Categories", mCategoryId);
        bundle.putSerializable("Callback", this);
        nextFrag.setArguments(bundle);
        mActivity.addFragment(mTab, R.id.ll_report, nextFrag, FragmentReportExpenseAnalysisCategory.Tag, true);
        LogUtils.logLeaveFunction(Tag);
    }

    /**
     * Start Fragment ReportEvent
     */
    private void showListAccounts() {
        LogUtils.logEnterFunction(Tag);
        FragmentReportSelectAccount nextFrag = new FragmentReportSelectAccount();
        Bundle bundle = new Bundle();
        bundle.putInt("Tab", mTab);
        bundle.putIntArray("Accounts", mAccountId);
        bundle.putSerializable("Callback", this);
        nextFrag.setArguments(bundle);
        mActivity.addFragment(mTab, R.id.ll_report, nextFrag, FragmentReportSelectAccount.Tag, true);
        LogUtils.logLeaveFunction(Tag);
    }

    /**
     * Start Fragment ReportEVITimeSelect
     */
    private void showListTime() {
        LogUtils.logEnterFunction(Tag);
        FragmentReportExpenseAnalysisTime nextFrag = new FragmentReportExpenseAnalysisTime();
        Bundle bundle = new Bundle();
        bundle.putInt("Tab", mTab);
        bundle.putInt("Time", mViewedBy);
        bundle.putSerializable("Callback", this);
        nextFrag.setArguments(bundle);
        mActivity.addFragment(mTab, R.id.ll_report, nextFrag, FragmentReportExpenseAnalysisTime.Tag, true);
        LogUtils.logLeaveFunction(Tag);
    } // End showListTime

}
