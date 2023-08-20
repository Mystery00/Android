package me.ghui.v2er.general;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.View;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ghui.v2er.util.Check;
import me.ghui.v2er.R;
import me.ghui.v2er.util.Utils;
import me.ghui.v2er.util.Voast;

/**
 * Created by ghui on 29/08/2017.
 */

public class ShareManager {

    private static final int THUMB_SIZE = 150;
    private ShareData mShareData;
    private Context mContext;
    private BottomSheetDialog mDialog;

    public ShareManager(ShareData shareData, Context context) {
        mShareData = shareData;
        mContext = context;
        mDialog = new BottomSheetDialog(context);
        mDialog.setContentView(R.layout.share_dialog);
        View rootview = mDialog.findViewById(R.id.share_dialog_rootview);
        ButterKnife.bind(this, rootview);
    }

    private static void shareText(String title, String link, Context context) {
        shareTo(null, title, link, context);
    }

    public static void shareTo(String packageName, String title, String link, Context context) {
        if (!Utils.isAppAvailable(packageName)) {
            Voast.show("The App not Installed");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,
                String.format("%s\n%s", title, link));
        if (Check.notEmpty(packageName)) {
            intent.setPackage(packageName);
        }
        context.startActivity(Intent.createChooser(intent, "分享方式"));
    }

    private static Bitmap getDefaultBitmap() {
        Bitmap bmp = BitmapFactory.decodeResource(App.get().getResources(), R.drawable.ic_v2er);
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
        bmp.recycle();
        return thumbBmp;
    }

//        final String appName = "org.telegram.messenger";

    private static byte[] bmpToByteArray(final Bitmap bmp) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
//        bmp.recycle();

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @OnClick({
            R.id.share_item_3,
            R.id.share_item_7, R.id.share_item_8,
            R.id.share_item_9})
    void onShareItemClicked(View view) {
        switch (view.getId()) {
            case R.id.share_item_3:
                shareTo(ThirdApp.TELEGRAM, mShareData.title, mShareData.link, mContext);
                break;
            case R.id.share_item_7:
                shareTo(ThirdApp.TWITTER, mShareData.title, mShareData.link, mContext);
                break;
            case R.id.share_item_8:
                Utils.copyToClipboard(mContext, String.format("%s\n%s", mShareData.title, mShareData.link));
                Voast.show("链接已拷贝成功");
                break;
            case R.id.share_item_9:
                shareText(mShareData.title, mShareData.link, mContext);
                break;
        }
        mDialog.dismiss();
    }

    public void showShareDialog() {
        mDialog.show();
    }

    public static class ShareData {
        public static final int SESSION = 0;
        public static final int CIRCLE = 1;
        public static final int FAVORITE = 2;
        private String title;
        private String content;
        private String img;
        private String link;
        private ShareData() {
        }
        @IntDef({SESSION, CIRCLE, FAVORITE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface SENCE {
        }

        public static class Builder {
            private String title;
            private String content;
            private String img;
            private String link;

            public Builder(String title) {
                this.title = title;
            }

            public Builder content(String content) {
                this.content = content;
                return this;
            }

            public Builder img(String img) {
                this.img = img;
                return this;
            }

            public Builder link(String link) {
                this.link = link;
                return this;
            }

            public ShareData build() {
                ShareData shareData = new ShareData();
                shareData.title = title;
                shareData.content = content;
                shareData.img = img;
                shareData.link = link;
                return shareData;
            }

        }
    }

}
