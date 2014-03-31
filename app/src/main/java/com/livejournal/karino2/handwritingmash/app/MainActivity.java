package com.livejournal.karino2.handwritingmash.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity {

    static final int DIALOG_ID_URL = 1;
    static final int ACTIVITY_ID_SEND = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    private Uri putFileToContentDB(File file) {
        ContentResolver resolver = getBaseContext().getContentResolver();

        ContentValues content = new ContentValues(4);

        content.put(MediaStore.Images.ImageColumns.TITLE, "Tegaki");
        content.put(MediaStore.Images.ImageColumns.DATE_ADDED,
                System.currentTimeMillis() / 1000);
        content.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        content.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content);
    }


    private File saveBitmap(Bitmap screen) throws IOException {
        File dir = getFileStoreDirectory();
        File result = new File(dir, newFileName());

        OutputStream stream = new FileOutputStream(result);
        screen.compress(Bitmap.CompressFormat.PNG, 80, stream);
        stream.close();
        return result;
    }

    public static String newFileName() {
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        String filename = timeStampFormat.format(new Date()) + ".png";
        return filename;
    }

    public static void ensureDirExist(File dir) throws IOException {
        if(!dir.exists()) {
            if(!dir.mkdir()){
                throw new IOException();
            }
        }
    }

    public static File getFileStoreDirectory() throws IOException {
        File dir = new File(Environment.getExternalStorageDirectory(), "HandwringMash");
        ensureDirExist(dir);
        return dir;
    }


    private void deleteAllFiles(File folder) {
        for(File file : folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if(pathname.isDirectory())
                    return false;
                return true;
            }
        })) {
            ContentResolver resolver = getBaseContext().getContentResolver();
            resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "? = ?",
                    new String[]{MediaStore.Images.ImageColumns.DATA,
                            file.getAbsolutePath()}
            );

            file.delete();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // show dialog.
        showDialog(DIALOG_ID_URL);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_ID_URL:
                return queryUrlDialog();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
            case DIALOG_ID_URL:
                urlDialog = dialog;
                reflectClipboard();
                return;
        }
        super.onPrepareDialog(id, dialog);
    }

    private void reflectClipboard() {
        EditText et = (EditText)urlDialog.findViewById(R.id.edit_url);
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        // String result = clipboardManager.getText().toString();
        if (clipboardManager.getPrimaryClip().getItemCount() == 0)
            return; // do nothing.
        ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
        String result = item.getText().toString();
        /*
        Uri uri = item.getUri();
        if(uri == null) {
            String deb = item.getText().toString();
            return; // do nothing.
        }
        */
        et.setText(result);
    }


    Dialog urlDialog;

    private Dialog queryUrlDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.url_dialog, null);

        /*
        setOnClickListener(view, R.id.button_clear, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText et = (EditText)urlDialog.findViewById(R.id.edit_url);
                et.setText("");
            }
        });
        */
        setOnClickListener(view, R.id.button_done, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText et = (EditText)urlDialog.findViewById(R.id.edit_url);
                Intent data = new Intent();
                data.putExtra("replace_key", et.getText().toString());
                setResult(RESULT_OK, data);
                finish();
            }
        });
        setOnClickListener(view, R.id.button_clipboard, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reflectClipboard();
            }

        });
        urlDialog = new AlertDialog.Builder(this).setTitle("Result Url")
                .setView(view)
                .create();
        return urlDialog;
    }

    private void setOnClickListener(View view, int rid, View.OnClickListener listener) {
        Button button = (Button)view.findViewById(rid);
        button.setOnClickListener(listener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            DrawingCanvas dc = getDrawingCanvas();
            Bitmap bitmap = dc.getBitmap();
            File file = null;
            try {
                deleteAllFiles(getFileStoreDirectory());
                file = saveBitmap(bitmap);
            } catch (IOException e) {
                showMessage("save fail: " + e.getMessage());
                return true;
            }
            Uri uri = putFileToContentDB(file);


            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivityForResult(Intent.createChooser(intent, "Share Image"), ACTIVITY_ID_SEND);

            return true;
        }
        if (id== R.id.action_clear) {
            DrawingCanvas dc = getDrawingCanvas();
            dc.resetCanvas();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private DrawingCanvas getDrawingCanvas() {
        return (DrawingCanvas)findViewById(R.id.canvas);
    }

}
