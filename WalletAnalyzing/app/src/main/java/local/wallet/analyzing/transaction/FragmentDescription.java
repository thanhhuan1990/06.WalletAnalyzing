package local.wallet.analyzing.transaction;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.Serializable;

import local.wallet.analyzing.R;
import local.wallet.analyzing.utils.LogUtils;
import local.wallet.analyzing.main.ActivityMain;

/**
 * Created by huynh.thanh.huan on 1/6/2016.
 */
public class FragmentDescription extends Fragment {
    public static int           mTab = 1;
    public static final String  Tag = "---[" + mTab + "]---Description";

    private ActivityMain        mActivity;

    public interface IUpdateDescription extends Serializable {
        void onDescriptionUpdated(String description);
    }

    private String              oldDescription;

    private EditText            etDescription;

    private IUpdateDescription  mCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle bundle       = this.getArguments();
        mTab                = bundle.getInt("Tab", mTab);
        oldDescription      = bundle.getString("Description", "");
        mCallback           = (IUpdateDescription) bundle.get("Callback");

        LogUtils.trace(Tag, "oldDescription = " + oldDescription);

        LogUtils.logLeaveFunction(Tag);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);
        LogUtils.logLeaveFunction(Tag);
        return inflater.inflate(R.layout.layout_fragment_description, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);

        super.onActivityCreated(savedInstanceState);

        mActivity           = (ActivityMain) getActivity();

        etDescription = (EditText) getView().findViewById(R.id.etDescription);
        etDescription.setText(oldDescription);
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

        /* Todo: Update ActionBar: Spinner TransactionType */
        LayoutInflater mInflater = LayoutInflater.from(getActivity());
        View mCustomView = mInflater.inflate(R.layout.action_bar_with_button_done, null);
        TextView tvTitle    = (TextView) mCustomView.findViewById(R.id.tvTitle);
        tvTitle.setText(getResources().getString(R.string.title_description));
        ImageView ivDone    = (ImageView) mCustomView.findViewById(R.id.ivDone);
        ivDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.trace(Tag, "Click Menu Action Done.");
                ((ActivityMain) getActivity()).hideKeyboard();

                mCallback.onDescriptionUpdated(etDescription.getText().toString());
                // Back
                getFragmentManager().popBackStackImmediate();
                }
            }

            );

            ((ActivityMain) getActivity()).updateActionBar(mCustomView);

            LogUtils.logLeaveFunction(Tag);
        }

    }
