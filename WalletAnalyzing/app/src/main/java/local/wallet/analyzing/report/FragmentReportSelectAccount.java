package local.wallet.analyzing.report;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import local.wallet.analyzing.R;
import local.wallet.analyzing.utils.LogUtils;
import local.wallet.analyzing.main.ActivityMain;
import local.wallet.analyzing.main.Configs;
import local.wallet.analyzing.model.Account;
import local.wallet.analyzing.model.AccountType;
import local.wallet.analyzing.model.Currency;
import local.wallet.analyzing.sqlite.helper.DatabaseHelper;

/**
 * Created by huynh.thanh.huan on 12/30/2015.
 */
public class FragmentReportSelectAccount extends Fragment implements View.OnClickListener {
    public static int               mTab = 4;
    public static final String      Tag = "---[" + mTab + "]---ReportSelectAccount";

    private ActivityMain            mActivity;

    public interface ISelectReportAccount extends Serializable {
        void onReportAccountSelected(int[] accountId);
    }

    private DatabaseHelper          mDbHelper;
    private Configs mConfigs;

    // List of selected Account from ReportEVI
    private int[]                   currentAccounts;

    private ToggleButton            tbAllAccount;
    private ListView                lvAccount;
    private AccountAdapter          accAdapter;
    private List<AccountItem>       arAccounts = new ArrayList<AccountItem>();

    private TextView                tvEmpty;

    private ISelectReportAccount    mCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle bundle   = this.getArguments();
        mTab            = bundle.getInt("Tab", mTab);
        currentAccounts = bundle.getIntArray("Accounts");
        mCallback       = (ISelectReportAccount) bundle.getSerializable("Callback");

        LogUtils.trace(Tag, "currentAccounts = " + Arrays.toString(currentAccounts));

        LogUtils.logLeaveFunction(Tag);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);
        LogUtils.logLeaveFunction(Tag);
        return inflater.inflate(R.layout.layout_fragment_report_select_accounts, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);
        super.onActivityCreated(savedInstanceState);

        mActivity       = (ActivityMain) getActivity();

        mDbHelper       = new DatabaseHelper(getActivity());
        mConfigs        = new Configs(getActivity());

        lvAccount       = (ListView) getView().findViewById(R.id.lvAccount);
        tvEmpty         = (TextView) getView().findViewById(R.id.tvEmpty);

        List<Account> listAccount     = mDbHelper.getAllAccounts();
        for(Account acc : listAccount) {
            boolean isSelected = false;
            for(int i = 0 ; i < currentAccounts.length; i++) {
                if(acc.getId() == currentAccounts[i]) {
                    isSelected = true;
                    break;
                }
            }
            arAccounts.add(new AccountItem(acc, isSelected));
        }

        accAdapter      = new AccountAdapter(getActivity(), arAccounts);
        lvAccount.setAdapter(accAdapter);

        lvAccount.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Todo: set check/uncheck
                arAccounts.get(position).isChecked = !arAccounts.get(position).isChecked;
                accAdapter.notifyDataSetChanged();

                // Todo: update tbAllAccounts
                boolean isAll = true;
                for(AccountItem account : arAccounts) {
                    if(!account.isChecked) {
                        isAll = false;
                    }
                }

                tbAllAccount.setChecked(isAll);
            }
        });

        if (listAccount.size() > 0) {
            tvEmpty = (TextView) getView().findViewById(R.id.tvEmpty);
            tvEmpty.setVisibility(View.GONE);
        }

        tbAllAccount    = (ToggleButton) getView().findViewById(R.id.tbAllAccount);
        tbAllAccount.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    for (AccountItem acc : arAccounts) {
                        acc.isChecked = true;
                    }

                    for (AccountItem acc : arAccounts) {
                        if(!acc.isChecked) {
                            tbAllAccount.setChecked(false);
                            break;
                        }
                    }
                    accAdapter.notifyDataSetChanged();
                }
            }
        });

        if(currentAccounts.length == arAccounts.size()) {
            tbAllAccount.setChecked(true);
        } else {
            tbAllAccount.setChecked(false);
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

        // Init ActionBar
        LayoutInflater mInflater    = LayoutInflater.from(getActivity());
        View mCustomView            = mInflater.inflate(R.layout.action_bar_with_button_done, null);
        TextView tvTitle            = (TextView) mCustomView.findViewById(R.id.tvTitle);
        tvTitle.setText(getResources().getString(R.string.title_report_evi_account));

        ImageView ivDone            = (ImageView) mCustomView.findViewById(R.id.ivDone);
        ivDone.setOnClickListener(this);

        ((ActivityMain) getActivity()).updateActionBar(mCustomView);

        LogUtils.logLeaveFunction(Tag);
    } // End onCreateOptionsMenu

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivDone: {
                List<Integer> arSelectedId = new ArrayList<Integer>();
                for(int i = 0; i < arAccounts.size(); i++) {
                    if(arAccounts.get(i).isChecked) {
                        arSelectedId.add(arAccounts.get(i).account.getId());
                    }
                }

                if(arSelectedId.size() > 0) {
                    int[] accounts = new int[arSelectedId.size()];
                    for(int i = 0 ; i < arSelectedId.size(); i++) {
                        accounts[i] = arSelectedId.get(i);
                    }

                    LogUtils.trace(Tag, "Accounts: " + Arrays.toString(accounts));

                    mCallback.onReportAccountSelected(accounts);

                    getFragmentManager().popBackStackImmediate();
                } else {
                    ((ActivityMain) getActivity()).showError(getResources().getString(R.string.Input_Error_account_none));
                }

                break;
            }
            default:
                break;
        }
    } // End onClick

    /**
     * Item account to show in ListView
     */
    private class AccountItem {
        Account account;
        boolean isChecked;

        public AccountItem(Account account, boolean isChecked) {
            this.account    = account;
            this.isChecked  = isChecked;
        }
    }

    /**
     * AccountAdapter: Adapter of account's listview
     */
    private class AccountAdapter extends ArrayAdapter<AccountItem> {

        private class ViewHolder {
            LinearLayout llAccount;
            ImageView       ivIcon;
            TextView        tvAccount;
            TextView        tvRemain;
            ImageView       ivUsing;
        }

        private List<AccountItem> arAccounts;

        public AccountAdapter(Context context, List<AccountItem> items) {
            super(context, R.layout.listview_item_account_select, items);
            this.arAccounts = items;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());

            final ViewHolder viewHolder;

            if (convertView == null || convertView.getTag() == null) {
                viewHolder = new ViewHolder();

                convertView = inflater.inflate(R.layout.listview_item_account_select, parent, false);
                viewHolder.llAccount    = (LinearLayout) convertView.findViewById(R.id.llAccount);
                viewHolder.ivIcon       = (ImageView) convertView.findViewById(R.id.ivIcon);
                viewHolder.tvAccount    = (TextView) convertView.findViewById(R.id.tvAccount);
                viewHolder.tvRemain     = (TextView) convertView.findViewById(R.id.tvRemain);
                viewHolder.ivUsing      = (ImageView) convertView.findViewById(R.id.ivUsing);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.ivIcon.setImageResource(AccountType.getAccountTypeById(arAccounts.get(position).account.getTypeId()).getIcon());
            viewHolder.tvAccount.setText(arAccounts.get(position).account.getName());

            Double remain = mDbHelper.getAccountRemain(arAccounts.get(position).account.getId());
            viewHolder.tvRemain.setText(Currency.formatCurrency(getContext(), mConfigs.getInt(Configs.Key.Currency), remain));

            if(arAccounts.get(position).isChecked) {
                viewHolder.ivUsing.setVisibility(View.VISIBLE);
            } else {
                viewHolder.ivUsing.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }

    }
}
