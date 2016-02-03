package local.wallet.analyzing;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import local.wallet.analyzing.Utils.LogUtils;
import local.wallet.analyzing.model.AccountType;
import local.wallet.analyzing.model.Currency;
import local.wallet.analyzing.sqlite.helper.DatabaseHelper;
import local.wallet.analyzing.model.Account;

/**
 * Created by huynh.thanh.huan on 12/30/2015.
 */
public class FragmentAccounts extends Fragment {
    private static final String TAG = "FragmentAccounts";

    private static final int NORMAL_MODE = 1;
    private static final int EDIT_MODE = 2;

    private int mCurrentMode = NORMAL_MODE;

    private DatabaseHelper db;
    private ListView lvAccount;
    private AccountAdapter accAdapter;
    private List<Account> listAccount = new ArrayList<Account>();

    private TextView tvEmpty;

    static FragmentAccounts newInstance() {
        FragmentAccounts fragmentAccounts = new FragmentAccounts();
        return fragmentAccounts;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logEnterFunction(TAG, null);
        LogUtils.logLeaveFunction(TAG, null, null);
        return inflater.inflate(R.layout.layout_fragment_accounts, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(TAG, null);

        super.onActivityCreated(savedInstanceState);

        db = new DatabaseHelper(getActivity());

        lvAccount = (ListView) getView().findViewById(R.id.lvAccount);
        tvEmpty = (TextView) getView().findViewById(R.id.tvEmpty);

        listAccount = db.getAllAccounts();
        accAdapter = new AccountAdapter(getActivity(), listAccount);
        lvAccount.setAdapter(accAdapter);

        lvAccount.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LogUtils.trace(TAG, "Click on Account number " + position + " -> ID = " + listAccount.get(position).getId());
                // Go to list of transaction related with this Account
                FragmentAccountTransactions nextFrag = new FragmentAccountTransactions();
                Bundle bundle = new Bundle();
                bundle.putString("Tag", ((ActivityMain) getActivity()).getFragment(ActivityMain.TAB_POSITION_ACCOUNTS).getTag());
                bundle.putInt("AccountID", accAdapter.getItem(position).getId());
                nextFrag.setArguments(bundle);
                FragmentAccounts.this.getFragmentManager().beginTransaction()
                        .add(R.id.layout_account, nextFrag, "FragmentAccountTransactions")
                        .addToBackStack(null)
                        .commit();

            }
        });

        if (listAccount.size() > 0) {
            tvEmpty = (TextView) getView().findViewById(R.id.tvEmpty);
            tvEmpty.setVisibility(View.GONE);
        }

        LogUtils.logLeaveFunction(TAG, null, null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogUtils.logEnterFunction(TAG, null);

        super.onCreateOptionsMenu(menu, inflater);

        /* Init ActionBar */
        LayoutInflater mInflater = LayoutInflater.from(getActivity());
        View mCustomView = mInflater.inflate(R.layout.action_bar_account, null);

        ImageView ivAdd = (ImageView) mCustomView.findViewById(R.id.ivAdd);
        ImageView ivEdit = (ImageView) mCustomView.findViewById(R.id.ivEdit);
        ImageView ivDone = (ImageView) mCustomView.findViewById(R.id.ivDone);

        if (mCurrentMode == NORMAL_MODE) {
            ivEdit.setVisibility(listAccount.size() > 0 ? View.VISIBLE : View.GONE);
            ivAdd.setVisibility(View.VISIBLE);
            ivDone.setVisibility(View.GONE);
        } else if (mCurrentMode == EDIT_MODE) {
            ivAdd.setVisibility(View.GONE);
            ivEdit.setVisibility(View.GONE);
            ivDone.setVisibility(View.VISIBLE);
        }

        ivAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.trace(TAG, "Click Menu Action Add Account.");
                FragmentAccountCreate nextFrag = new FragmentAccountCreate();
                FragmentAccounts.this.getFragmentManager().beginTransaction()
                        .add(R.id.layout_account, nextFrag, "FragmentAccountCreate")
                        .addToBackStack(null)
                        .commit();
            }
        });

        ivEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.trace(TAG, "Click Menu Action Edit.");
                accAdapter.notifyDataSetChanged();
                mCurrentMode = EDIT_MODE;
                getActivity().invalidateOptionsMenu();
            }
        });

        ivDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.trace(TAG, "Click Menu Action Done.");
                accAdapter.notifyDataSetChanged();
                mCurrentMode = NORMAL_MODE;
                getActivity().invalidateOptionsMenu();
            }
        });

        ((ActivityMain) getActivity()).updateActionBar(mCustomView);

        LogUtils.logLeaveFunction(TAG, null, null);
    }

    /**
     * Update AccountType, call from ActivityMain
     */
    public void addToAccountList(Account account) {
        LogUtils.logEnterFunction(TAG, null);

        listAccount.add(account);
        // Reload list Account
        accAdapter.notifyDataSetChanged();

        tvEmpty.setVisibility(View.GONE);

        LogUtils.logLeaveFunction(TAG, null, null);
    }

    public void updateToAccountList(Account account) {
        LogUtils.logEnterFunction(TAG, null);

        for (int i = 0; i < listAccount.size(); i++) {
            if (listAccount.get(i).getId() == account.getId()) {
                listAccount.set(i, account);
            }
        }
        // Reload list Account
        accAdapter.notifyDataSetChanged();

        LogUtils.logLeaveFunction(TAG, null, null);
    }

    public void removeToAccountList(int accountId) {
        LogUtils.logEnterFunction(TAG, null);

        // Remove Account
        for (Account account : listAccount) {
            if (account.getId() == accountId) {
                listAccount.remove(account);
            }
        }
        // Reload list Account
        accAdapter.notifyDataSetChanged();

        if (listAccount.size() > 0) {
            tvEmpty.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
        }

        LogUtils.logLeaveFunction(TAG, null, null);
    }

    /**
     * Adapter for Accounts
     */
    private class AccountAdapter extends ArrayAdapter<Account> {
        private class ViewHolder {
            ImageView ivIcon;
            TextView tvAccountName;
            TextView tvRemain;
            ImageView ivCurrency;
            ImageView ivEdit;
        }

        private List<Account> mList;

        public AccountAdapter(Context context, List<Account> items) {
            super(context, R.layout.listview_item_account, items);
            this.mList = items;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Account getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder; // view lookup cache stored in tag
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.listview_item_account, parent, false);
                viewHolder.ivIcon = (ImageView) convertView.findViewById(R.id.ivIcon);
                viewHolder.tvAccountName = (TextView) convertView.findViewById(R.id.tvAccount);
                viewHolder.tvRemain = (TextView) convertView.findViewById(R.id.tvRemain);
                viewHolder.ivEdit = (ImageView) convertView.findViewById(R.id.ivEdit);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.ivIcon.setImageResource(AccountType.getAccountTypeById(mList.get(position).getTypeId()).getIcon());
            viewHolder.tvAccountName.setText(mList.get(position).getName());

            Double remain = db.getAccountRemain(mList.get(position).getId());
            viewHolder.tvRemain.setText(Currency.formatCurrency(getContext(), Currency.getCurrencyById(mList.get(position).getCurrencyId()), remain));

            if (mCurrentMode == NORMAL_MODE) {
                viewHolder.ivEdit.setImageResource(R.drawable.icon_list_edit);
                viewHolder.ivEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LogUtils.trace(TAG, "Edit item number " + position + " -> AccountID = " + listAccount.get(position));
                        FragmentAccountUpdate nextFrag = new FragmentAccountUpdate();
                        Bundle bundle = new Bundle();
                        bundle.putInt("AccountID", listAccount.get(position).getId());
                        nextFrag.setArguments(bundle);
                        FragmentAccounts.this.getFragmentManager().beginTransaction()
                                .add(R.id.layout_account, nextFrag, "FragmentAccountUpdate")
                                .addToBackStack(null)
                                .commit();
                    }
                });
            } else if (mCurrentMode == EDIT_MODE) {
                viewHolder.ivEdit.setImageResource(R.drawable.icon_list_delete);
                viewHolder.ivEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LogUtils.trace(TAG, "Delete item number " + position + " -> AccountID = " + listAccount.get(position));
                        db.deleteAccount(listAccount.get(position).getId());
                        listAccount.remove(position);
                        accAdapter.notifyDataSetChanged();

                        if (listAccount.size() == 0) {
                            mCurrentMode = NORMAL_MODE;
                            getActivity().invalidateOptionsMenu();
                        }
                    }
                });
            }

            return convertView;
        }
    }
}