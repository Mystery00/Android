package me.ghui.v2er.module.topic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.orhanobut.logger.Logger;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import me.ghui.v2er.R;
import me.ghui.v2er.adapter.base.MultiItemTypeAdapter;
import me.ghui.v2er.adapter.base.ViewHolder;
import me.ghui.v2er.general.Navigator;
import me.ghui.v2er.general.PreConditions;
import me.ghui.v2er.injector.component.DaggerTopicComponent;
import me.ghui.v2er.injector.module.TopicModule;
import me.ghui.v2er.module.base.BaseActivity;
import me.ghui.v2er.module.user.UserHomeActivity;
import me.ghui.v2er.network.bean.SimpleInfo;
import me.ghui.v2er.network.bean.TopicInfo;
import me.ghui.v2er.util.ScaleUtils;
import me.ghui.v2er.util.UriUtils;
import me.ghui.v2er.util.Utils;
import me.ghui.v2er.widget.AndroidBug5497Workaround;
import me.ghui.v2er.widget.KeyboardDetectorRelativeLayout;
import me.ghui.v2er.widget.LoadMoreRecyclerView;


/**
 * Created by ghui on 04/05/2017.
 */

public class TopicActivity extends BaseActivity<TopicContract.IPresenter> implements TopicContract.IView,
        LoadMoreRecyclerView.OnLoadMoreListener, MultiItemTypeAdapter.OnItemClickListener, KeyboardDetectorRelativeLayout.IKeyboardChanged {
    private static final String TOPIC_ID_KEY = KEY("topic_id_key");

    @BindView(R.id.common_recyclerview)
    LoadMoreRecyclerView mLoadMoreRecyclerView;
    @BindView(R.id.topic_reply_wrapper)
    KeyboardDetectorRelativeLayout mReplyWrapper;
    @BindView(R.id.topic_inner_reply_wrapper)
    RelativeLayout mReplyInnerWrapper;
    @BindView(R.id.topic_reply_et)
    EditText mReplyEt;
    @BindView(R.id.reply_fab_btn)
    FloatingActionButton mReplyFabBtn;

    @Inject
    LoadMoreRecyclerView.Adapter mAdapter;
    private String mTopicId;
    private TopicInfo mTopicInfo;
    private MenuItem mLoveMenuItem;
    private MenuItem mThxMenuItem;
    private BottomSheetDialog mBottomSheetDialog;
    private OnBottomDialogItemClickListener mBottomSheetDialogItemClickListener;


    public static void openById(String topicId, Context context) {
        Navigator.from(context)
                .to(TopicActivity.class)
                .putExtra(TopicActivity.TOPIC_ID_KEY, topicId)
                .start();
    }

    public static void open(String link, Context context) {
        openById(UriUtils.getLastSegment(link), context);
    }

    @Override
    protected int attachLayoutRes() {
        return R.layout.act_topic_info_page;
    }

    @Override
    protected void startInject() {
        DaggerTopicComponent.builder()
                .appComponent(getAppComponent())
                .topicModule(new TopicModule(this))
                .build().inject(this);
    }

    @Override
    protected void parseExtras(Intent intent) {
        mTopicId = intent.getStringExtra(TOPIC_ID_KEY);
    }

    @Override
    protected void configSystemBars(Window window) {
        Utils.transparentBars(window, Color.TRANSPARENT, getColor(R.color.transparent_navbar_color));
    }

    @Override
    protected void configToolBar(Toolbar toolBar) {
        super.configToolBar(toolBar);
        Utils.setPaddingForStatusBar(toolBar);
        mToolbar.inflateMenu(R.menu.topic_info_toolbar_menu);
        mLoveMenuItem = mToolbar.getMenu().findItem(R.id.action_star);
        mThxMenuItem = mToolbar.getMenu().findItem(R.id.action_thx);
        mToolbar.setOnMenuItemClickListener(item -> {
            if (mTopicInfo == null) {
                toast("请等到加载完成");
                return true;
            }
            TopicInfo.HeaderInfo headerInfo = mTopicInfo.getHeaderInfo();
            switch (item.getItemId()) {
                case R.id.action_star:
                    if (headerInfo.hadStared()) {
                        mPresenter.unStarTopic(mTopicId, headerInfo.getT());
                    } else {
                        mPresenter.starTopic(mTopicId, headerInfo.getT());
                    }
                    break;
                case R.id.action_thx:
                    if (!headerInfo.hadThanked()) {
                        mPresenter.thxCreator(mTopicId, headerInfo.getT());
                    } else {
                        toast(R.string.already_thx_cannot_return);
                        return true;
                    }
                    break;
                case R.id.action_block:
                    mPresenter.ignoreTopic(mTopicId, mTopicInfo.getOnce());
                    break;
            }
            return true;
        });
    }

    @Override
    protected void init() {
        AndroidBug5497Workaround.assistActivity(this);
        Utils.setPaddingForNavbar(mReplyWrapper);
        mReplyWrapper.addKeyboardStateChangedListener(this);
        mLoadMoreRecyclerView.addDivider();
        mLoadMoreRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mLoadMoreRecyclerView.setAdapter(mAdapter);
        mLoadMoreRecyclerView.setOnLoadMoreListener(this);
        mAdapter.setOnItemClickListener(this);
        mLoadMoreRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (mReplyWrapper.getVisibility() == View.VISIBLE) {
                        animateEditInnerWrapper(false);
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                }
            }
        });
    }

    @Override
    protected PtrHandler attachPtrHandler() {
        return new PtrDefaultHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                mLoadMoreRecyclerView.resetWillLoadPage();
                mPresenter.loadData(mTopicId);
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return super.checkCanDoRefresh(frame, mLoadMoreRecyclerView, header);
            }
        };
    }

    @Override
    public void onLoadMore(int willLoadPage) {
        mPresenter.loadData(mTopicId, willLoadPage);
    }

    @Override
    public void fillView(TopicInfo topicInfo, boolean isLoadMore) {
        mTopicInfo = topicInfo;
        if (topicInfo == null) {
            mAdapter.setData(null);
            return;
        }
        mAdapter.setData(topicInfo.getItems(isLoadMore), isLoadMore);
        mLoadMoreRecyclerView.setHasMore(topicInfo.getTotalPage());
        updateStarStatus(mTopicInfo.getHeaderInfo().hadStared(), false);
        updateThxCreatorStatus(mTopicInfo.getHeaderInfo().hadThanked(), false);
    }

    @OnClick(R.id.reply_fab_btn)
    void onNewReplyFlbClicked(FloatingActionButton button) {
        button.hide();
        animateEditInnerWrapper(true);
    }


    void animateEditInnerWrapper(boolean isShow) {
        int deltaX = ScaleUtils.dp(20);
        int deltaY = ScaleUtils.dp(20);
        int cX = (int) (ScaleUtils.getScreenW() - ScaleUtils.dp(56) - ScaleUtils.dp(16) - deltaX);
        int cY = ScaleUtils.dp(48) / 2;
        int startRadius = ScaleUtils.dp(40);
        int endRadius = (int) ScaleUtils.getScreenW();
        if (isShow) {//show edit wrapper
            mReplyFabBtn.animate()
                    .xBy(-deltaX)
                    .yBy(deltaY)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Animator animator = ViewAnimationUtils.createCircularReveal(mReplyInnerWrapper, cX, cY, startRadius, endRadius);
                            animator.setDuration(350);
                            animator.start();
                            mReplyWrapper.setVisibility(View.VISIBLE);
                            mReplyFabBtn.hide();
                        }
                    }).start();
        } else {//hide wrapper
            if (mReplyWrapper.getVisibility() != View.VISIBLE) return;
            Animator animator = ViewAnimationUtils.createCircularReveal(mReplyInnerWrapper, cX, cY, endRadius, startRadius);
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mReplyWrapper.setVisibility(View.INVISIBLE);
                    mReplyFabBtn.show();
                    mReplyFabBtn.animate()
                            .xBy(deltaX)
                            .yBy(-deltaY)
                            .setListener(null)
                            .setDuration(100)
                            .start();
                }
            });
            animator.start();
        }
    }


    private void updateStarStatus(boolean isStared, boolean needUpdateData) {
        mLoveMenuItem.setIcon(isStared ?
                R.drawable.ic_star_selected : R.drawable.ic_star_normal);
        if (needUpdateData) {
            mTopicInfo.getHeaderInfo().updateStarStatus(isStared);
        }
    }

    private void updateThxCreatorStatus(boolean thxed, boolean needUpdateData) {
        mThxMenuItem.setTitle(thxed ? getString(R.string.thx_already_send) : getString(R.string.thx_str));
        if (needUpdateData) {
            mTopicInfo.getHeaderInfo().updateThxStatus(thxed);
        }
    }

    @Override
    public void afterStarTopic(TopicInfo topicInfo) {
        mTopicInfo.getHeaderInfo().setFavoriteLink(topicInfo.getHeaderInfo().getFavoriteLink());
        updateStarStatus(mTopicInfo.getHeaderInfo().hadStared(), true);
        toast("收藏成功");
    }

    @Override
    public void afterUnStarTopic(TopicInfo topicInfo) {
        mTopicInfo.getHeaderInfo().setFavoriteLink(topicInfo.getHeaderInfo().getFavoriteLink());
        updateStarStatus(mTopicInfo.getHeaderInfo().hadStared(), true);
        toast("取消收藏成功");
    }

    @Override
    public void afterThxCreator(boolean success) {
        if (success) {
            updateThxCreatorStatus(true, true);
            toast(R.string.thx_already_send);
        } else {
            toast(getString(R.string.send_thx_occured_error));
        }
    }

    @Override
    public void afterIgnoreTopic(boolean success) {
        if (success) {
            toast("主题已忽略");
            finish();
        } else {
            toast("忽略主题遇到问题");
        }
    }

    @Override
    public void afterIgnoreReply(boolean success, int position) {
        if (success) {
            toast("已忽略");
            mAdapter.getDatas().remove(position);
            mAdapter.notifyDataSetChanged();
        } else {
            toast("忽略回复遇到问题");
        }
    }

    @Override
    public void afterReplyTopic(TopicInfo topicInfo) {
        fillView(topicInfo, false);
        mReplyEt.setText(null);
        toast("回复成功");
    }

    @OnClick(R.id.reply_send_btn)
    void onPostBtnClicked() {
        CharSequence text = mReplyEt.getText();
        if (PreConditions.isEmpty(text)) {
            toast("回复不能为空");
            return;
        }
        mPresenter.replyTopic(mTopicId, mTopicInfo.toReplyMap(text.toString()));
    }

    @Override
    public void onItemClick(View view, ViewHolder holder, int position) {
        if (mBottomSheetDialog == null) {
            mBottomSheetDialog = new BottomSheetDialog(getContext());
            mBottomSheetDialog.getWindow().setNavigationBarColor(Color.WHITE);
            mBottomSheetDialog.setContentView(R.layout.topic_reply_dialog_item);
            ViewGroup parentView = (ViewGroup) mBottomSheetDialog.findViewById(R.id.topic_reply_dialog_rootview);
            mBottomSheetDialogItemClickListener = new OnBottomDialogItemClickListener();
            for (int i = 0; i < parentView.getChildCount(); i++) {
                parentView.getChildAt(i).setOnClickListener(mBottomSheetDialogItemClickListener);
            }
        }
        mBottomSheetDialogItemClickListener.setPosition(position);
        mBottomSheetDialog.show();
    }

    @Override
    public void onKeyboardShown() {
        Logger.d("onKeyboardShown");
        mReplyWrapper.setPadding(mReplyWrapper.getPaddingLeft(), mReplyWrapper.getPaddingTop(), mReplyWrapper.getPaddingRight(), 0);
    }

    @Override
    public void onKeyboardHidden() {
        Logger.d("onKeyboardHidden");
        Utils.setPaddingForNavbar(mReplyWrapper);
    }

    private class OnBottomDialogItemClickListener implements View.OnClickListener {
        private TopicInfo.Reply item;
        private int position;

        public void setPosition(int position) {
            this.position = position;
            this.item = (TopicInfo.Reply) mAdapter.getItem(position);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.reply_dialog_btn1:
                    //reply to the comment
                    animateEditInnerWrapper(true);
                    mReplyEt.setText("@" + item.getUserName() + " ");
                    mReplyEt.setSelection(mReplyEt.getText().length());
                    Utils.toggleKeyboard(true, mReplyEt);
                    break;
                case R.id.reply_dialog_btn2:
                    //copy reply to clipboard
                    Utils.copyToClipboard(TopicActivity.this, Html.fromHtml(item.getReplyContent()).toString());
                    toast("拷贝成功");
                    break;
                case R.id.reply_dialog_btn3:
                    //ignore reply
                    mPresenter.ignoreReply(position, item.getReplyId(), mTopicInfo.getOnce());
                    break;
                case R.id.reply_dialog_btn4:
                    //homepage
                    UserHomeActivity.open(item.getUserName(), TopicActivity.this);
                    break;
            }
            mBottomSheetDialog.dismiss();
        }
    }

}
