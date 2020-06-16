package org.shench.h5androiddemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.shench.h5androiddemo.manage.impl.JavaScriptImpl;
import org.shench.h5androiddemo.runtimepermissions.PermissionsManager;
import org.shench.h5androiddemo.runtimepermissions.PermissionsResultAction;
import org.shench.h5androiddemo.utils.LogUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int FILECHOOSER_RESULTCODE = 1;// 表单的结果回调
    private static final int REQ_CAMERA = FILECHOOSER_RESULTCODE + 1;//拍照
    private final static int REQ_VIDEO = REQ_CAMERA + 1;// 录像
    private final static int REQ_PHOTO = REQ_VIDEO + 1;// 相册
    private WebView mWebView;
    private ValueCallback<Uri[]> mUploadCallbackAboveL;
    private ValueCallback<Uri> mUploadMessage;// 表单的数据信息
    private boolean mVideoFlag = false;
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        requestPermissions();
        initWebView();
    }

    private void requestPermissions() {
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                Toast.makeText(MainActivity.this, "All permissions have been granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(String permission) {
                Toast.makeText(MainActivity.this, "Permission " + permission + " has been denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initWebView() {
        mWebView = findViewById(R.id.web_view);
        mWebView.clearCache(true);// 清空缓存
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);// 让WebView能够执行javaScript
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        settings.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true
        // 是否允许通过file url加载的Javascript读取本地文件，默认值 false
        settings.setGeolocationEnabled(true);
        settings.setSupportZoom(true);// 支持缩放(适配到当前屏幕)
        settings.setUseWideViewPort(true);      // 将图片调整到合适的大小
        settings.setDisplayZoomControls(true);   // 设置可以被显示的屏幕控制
        settings.setDefaultFontSize(12);   // 设置默认字体大小
        mWebView.addJavascriptInterface(new JavaScriptImpl(), "android");


        //实现：WebView里的链接，都在自身打开，不调用系统浏览器
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                LogUtil.d(TAG, "shouldOverrideUrlLoading===url=" + url);
                if (Build.VERSION.SDK_INT < 26) {
                    view.loadUrl(url);
                    return true;
                } else {
                    return false;
                }
            }

            //在开始加载网页时会回调
            @Override
            public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
                super.onPageStarted(webView, s, bitmap);
                LogUtil.d(TAG, "onPageStarted");
            }

            //加载错误的时候会回调
            @Override
            public void onReceivedError(WebView webView, int i, String s, String s1) {
                super.onReceivedError(webView, i, s, s1);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return;
                }
                LogUtil.d(TAG, "onReceivedError1");
            }

            //加载错误的时候会回调
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
                super.onReceivedError(webView, webResourceRequest, webResourceError);
                LogUtil.d(TAG, "onReceivedError2");
            }

            //加载完成的时候会回调
            @Override
            public void onPageFinished(WebView webView, String s) {
                super.onPageFinished(webView, s);
                LogUtil.d(TAG, "onPageFinished");
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {

            // For Android >= 5.0
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                LogUtil.d(TAG, "onShowFileChooser");
                mUploadCallbackAboveL = filePathCallback;
                if (fileChooserParams.isCaptureEnabled()) {
                    String[] acceptTypes = fileChooserParams.getAcceptTypes();
                    LogUtil.d(TAG, "acceptTypes=" + acceptTypes);
                    for (int i = 0; i < acceptTypes.length; i++) {
                        if (acceptTypes[i].contains("video")) {
                            mVideoFlag = true;
                            break;
                        }
                    }
                    if (mVideoFlag) {
                        recordVideo();
                        mVideoFlag = false;
                    } else {
                        takePhoto();
                    }
                } else {
                    pickPhoto();
                }
                return true;
            }

            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                LogUtil.d(TAG, "openFileChooser1");
                mUploadMessage = uploadMsg;
                pickPhoto();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                LogUtil.d(TAG, "openFileChooser2");
                mUploadMessage = uploadMsg;
                mVideoFlag = acceptType.contains("video");
                if (mVideoFlag) {
                    recordVideo();
                    mVideoFlag = false;
                } else {
                    pickPhoto();
                }
            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                LogUtil.d(TAG, "openFileChooser3");
                mUploadMessage = uploadMsg;
                if (!TextUtils.isEmpty(capture)) {
                    mVideoFlag = acceptType.contains("video");
                    if (mVideoFlag) {
                        recordVideo();
                        mVideoFlag = false;
                    } else {
                        takePhoto();
                    }
                } else {
                    pickPhoto();
                }
            }
        });
        mWebView.loadUrl("file:///android_asset/test.html");
    }
    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + "images" + File.separator;
        File fileUri = new File(path, SystemClock.currentThreadTimeMillis() + ".jpg");
        if (!fileUri.getParentFile().exists()) {
            fileUri.getParentFile().mkdirs();
        }
        mImageUri = Uri.fromFile(fileUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", fileUri);//通过FileProvider创建一个content类型的Uri
        }
        LogUtil.d(TAG, "mImageUri=" + mImageUri);
        //调用系统相机
        Intent intentCamera = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intentCamera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        intentCamera.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        //将拍照结果保存至photo_file的Uri中，不保留在相册中
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(intentCamera, REQ_CAMERA);
    }

    /**
     * 录像
     */
    private void recordVideo() {
        String path = getExternalFilesDir(Environment.DIRECTORY_MOVIES) + File.separator + "video" + File.separator;
        File fileUri = new File(path, SystemClock.currentThreadTimeMillis() + ".mp4");
        if (!fileUri.getParentFile().exists()) {
            fileUri.getParentFile().mkdirs();
        }
        mImageUri = Uri.fromFile(fileUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", fileUri);//通过FileProvider创建一个content类型的Uri
        }
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);  // 表示跳转至相机的录视频界面
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0.5);    // MediaStore.EXTRA_VIDEO_QUALITY 表示录制视频的质量，从 0-1，越大表示质量越好，同时视频也越大
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);    // 表示录制完后保存的录制，如果不写，则会保存到默认的路径，在onActivityResult()的回调，通过intent.getData中返回保存的路径
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);   // 设置视频录制的最长时间
        startActivityForResult(intent, REQ_VIDEO);  // 跳转
    }

    //激活相册操作
    private void pickPhoto() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d(TAG, "onActivityResult===data=" + data);
        if (requestCode == REQ_CAMERA || requestCode == REQ_VIDEO || requestCode == REQ_PHOTO) {
            if (null == mUploadMessage && null == mUploadCallbackAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (mUploadCallbackAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
        LogUtil.d(TAG, "onActivityResultAboveL===data=" + data);
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                results = new Uri[]{mImageUri};
            } else {
                String dataString = data.getDataString();
                LogUtil.d(TAG, "onActivityResultAboveL===dataString=" + dataString);
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        if (results != null) {
            mUploadCallbackAboveL.onReceiveValue(results);
        } else {
            results = new Uri[]{};
            mUploadCallbackAboveL.onReceiveValue(results);
        }
        mUploadCallbackAboveL = null;
    }
}
