package com.tttrtclive.ui.beauty;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tttrtclive.R;
import com.tttrtclive.ui.beauty.seekbar.DiscreteSeekBar;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import project.android.imageprocessing.entity.Effect;

public class BeautyPopup extends PopupWindow implements View.OnTouchListener,
        View.OnClickListener, View.OnKeyListener, DiscreteSeekBar.OnProgressChangeListener {
    private static final String TAG = "BeautyPopup";
    private TextView mTvMeiyan;
    private TextView mTvTiezhi;
    private DiscreteSeekBar mMopiSeekBar;
    private DiscreteSeekBar mMeibaiSeekBar;
    private DiscreteSeekBar mShoulianSeekBar;
    private DiscreteSeekBar mDayanSeekBar;
    private LinearLayout mRlMeiyan;
    private RelativeLayout mRlRoot;
    private RelativeLayout mRlTiezhi;
    private RecyclerView mFilterRecycleView;
    private EffectRecyclerAdapter mEffectRecyclerAdapter;

    private Context mContext;
    private View mRootView;
    private ArrayList<Effect> mEffects;
    private int mPositionSelect = 0;
    private TTTRtcEngine mTTTEngine;

    public BeautyPopup(Context context, TTTRtcEngine mTTTEngine) {
        this.mContext = context;
        this.mTTTEngine = mTTTEngine;

        initData();
        initViews();
    }

    private void initData() {
//        mEffects = EffectEnum.getEffectsByEffectType(Effect.EFFECT_TYPE_NORMAL);
        mEffects = new ArrayList<>();
        mEffects.add(new Effect("none", R.drawable.ic_delete_all, "none", 1, Effect.EFFECT_TYPE_NONE, ""));
        mEffects.add(new Effect("fengya_ztt_fu", R.drawable.fengya_ztt_fu, "normal/fengya_ztt_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""));
        mEffects.add(new Effect("hudie_lm_fu", R.drawable.hudie_lm_fu, "normal/hudie_lm_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""));
        mEffects.add(new Effect("touhua_ztt_fu", R.drawable.touhua_ztt_fu, "normal/touhua_ztt_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""));
        mEffects.add(new Effect("juanhuzi_lm_fu", R.drawable.juanhuzi_lm_fu, "normal/juanhuzi_lm_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""));
        mEffects.add(new Effect("mask_hat", R.drawable.mask_hat, "normal/mask_hat.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""));
        mEffects.add(new Effect("yazui", R.drawable.yazui, "normal/yazui.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""));
        mEffects.add(new Effect("yuguan", R.drawable.yuguan, "normal/yuguan.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""));
    }

    private void initViews() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mRootView = inflater.inflate(R.layout.popup_beauty, null);
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        this.setFocusable(true);
//        ColorDrawable dw = new ColorDrawable(0x00000000);
//        this.setBackgroundDrawable(dw);
        //设置动画效果
        this.setAnimationStyle(R.style.MyPopupWindow_anim_style);
        setContentView(mRootView);
        mRlRoot = (RelativeLayout) mRootView.findViewById(R.id.rl_root);
        mTvMeiyan = (TextView) mRootView.findViewById(R.id.tv_meiyan);
        mTvTiezhi = (TextView) mRootView.findViewById(R.id.tv_tiezhi);
        mMopiSeekBar = (DiscreteSeekBar) mRootView.findViewById(R.id.mopi_seek_bar);
        mMeibaiSeekBar = (DiscreteSeekBar) mRootView.findViewById(R.id.meibai_seek_bar);
        mShoulianSeekBar = (DiscreteSeekBar) mRootView.findViewById(R.id.shoulian_seek_bar);
        mDayanSeekBar = (DiscreteSeekBar) mRootView.findViewById(R.id.dayan_seek_bar);
        mRlMeiyan = (LinearLayout) mRootView.findViewById(R.id.rl_meiyan);
        mRlTiezhi = (RelativeLayout) mRootView.findViewById(R.id.rl_tiezhi);
        mFilterRecycleView = (RecyclerView) mRootView.findViewById(R.id.filter_recycle_view);
        mFilterRecycleView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mFilterRecycleView.setAdapter(mEffectRecyclerAdapter = new EffectRecyclerAdapter());

        mRlRoot.setOnClickListener(this);
        mTvMeiyan.setOnClickListener(this);
        mTvTiezhi.setOnClickListener(this);
        mMopiSeekBar.setOnProgressChangeListener(this);
        mMeibaiSeekBar.setOnProgressChangeListener(this);
        mShoulianSeekBar.setOnProgressChangeListener(this);
        mDayanSeekBar.setOnProgressChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.rl_root) {
            dismiss();
        } else if (i == R.id.tv_meiyan) {
            mRlMeiyan.setVisibility(View.VISIBLE);
            mRlTiezhi.setVisibility(View.INVISIBLE);
            mTvMeiyan.setTextColor(mContext.getResources().getColor(R.color.title_bar_bg));
            mTvTiezhi.setTextColor(mContext.getResources().getColor(R.color.color_desc));
        } else if (i == R.id.tv_tiezhi) {
            mRlMeiyan.setVisibility(View.INVISIBLE);
            mRlTiezhi.setVisibility(View.VISIBLE);
            mTvMeiyan.setTextColor(mContext.getResources().getColor(R.color.color_desc));
            mTvTiezhi.setTextColor(mContext.getResources().getColor(R.color.title_bar_bg));
        } else {

        }
    }

    //点击外部popup消失
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int height = mRootView.findViewById(R.id.rl_root).getTop();
        int y = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (y < height) {
                dismiss();
            }
        }
        return true;
    }

    //点back键消失
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && this.isShowing()) {
            this.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
        int i = seekBar.getId();
        float valueF = 1.0f * (value - seekBar.getMin()) / 100;
        if (i == R.id.mopi_seek_bar) {
            mTTTEngine.setBlurLevel((int) (valueF * 6));
        } else if (i == R.id.meibai_seek_bar) {
            mTTTEngine.setColorLevel(valueF);
        } else if (i == R.id.shoulian_seek_bar) {
            mTTTEngine.setCheekThinning(valueF);
        } else if (i == R.id.dayan_seek_bar) {
            mTTTEngine.setEyeEnlarging(valueF);
        } else {
        }
    }

    @Override
    public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
    }

    class EffectRecyclerAdapter extends RecyclerView.Adapter<EffectRecyclerAdapter.HomeRecyclerHolder> {

        @Override
        public HomeRecyclerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new HomeRecyclerHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_effect_recycler, parent, false));
        }

        @Override
        public void onBindViewHolder(HomeRecyclerHolder holder, final int position) {
            holder.effectImg.setImageResource(mEffects.get(position).resId());
            holder.effectImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPositionSelect == position) {
                        return;
                    }
                    Effect click = mEffects.get(mPositionSelect = position);
                    mTTTEngine.onEffectSelected(click);
                    notifyDataSetChanged();
                }
            });
            if (mPositionSelect == position) {
                holder.effectImg.setBackgroundResource(R.drawable.effect_select);
            } else {
                holder.effectImg.setBackgroundResource(0);
            }
        }

        @Override
        public int getItemCount() {
            return mEffects.size();
        }

        class HomeRecyclerHolder extends RecyclerView.ViewHolder {

            CircleImageView effectImg;

            public HomeRecyclerHolder(View itemView) {
                super(itemView);
                effectImg = (CircleImageView) itemView.findViewById(R.id.effect_recycler_img);
            }
        }
    }
}
