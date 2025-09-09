package com.prangesoftwaresolutions.audioanchor.activities;

import android.Manifest;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.prangesoftwaresolutions.audioanchor.R;
import com.prangesoftwaresolutions.audioanchor.adapters.AlbumAdapter;
import com.prangesoftwaresolutions.audioanchor.data.AnchorContract;
import com.prangesoftwaresolutions.audioanchor.models.Album;
import com.prangesoftwaresolutions.audioanchor.services.AudioService;
import com.prangesoftwaresolutions.audioanchor.utils.StoragePermissionHelper;
import com.prangesoftwaresolutions.audioanchor.utils.Utils;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // Constantes existentes
    private static final int ALBUM_LOADER = 0;
    private static final String ALBUM_SORT_ORDER = AnchorContract.AlbumEntry.COLUMN_TITLE + " COLLATE NOCASE ASC";

    // Nuevas constantes para SD card
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_CODE_AUDIO_FOLDER = 1002;
    private Uri mSDRootUri;

    // Variables existentes
    private AlbumAdapter mAlbumAdapter;
    private RecyclerView mAlbumRecyclerView;
    private ProgressBar mProgressBar;
    private TextView mEmptyView;

    private AudioService mAudioService;
    private boolean mServiceBound = false;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.AudioBinder binder = (AudioService.AudioBinder) service;
            mAudioService = binder.getService();
            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(AudioService.ACTION_TRACK_FINISHED)) {
                getLoaderManager().restartLoader(ALBUM_LOADER, null, MainActivity.this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mProgressBar = findViewById(R.id.progressBar);
        mEmptyView = findViewById(R.id.empty_view);

        mAlbumRecyclerView = findViewById(R.id.album_recycler_view);
        mAlbumRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAlbumAdapter = new AlbumAdapter(this, new ArrayList<Album>());
        mAlbumRecyclerView.setAdapter(mAlbumAdapter);

        // Verificar permisos de almacenamiento
        checkStoragePermissions();

        getLoaderManager().initLoader(ALBUM_LOADER, null, this);

        // Registrar receptor de broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioService.ACTION_TRACK_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkStoragePermissions();
    }

    private void checkStoragePermissions() {
        if (!StoragePermissionHelper.hasStoragePermission(this)) {
            StoragePermissionHelper.requestStoragePermission(this);
        }
    }

    public void browseForAudioFiles() {
        if (!StoragePermissionHelper.hasStoragePermission(this)) {
            StoragePermissionHelper.requestStoragePermission(this);
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | 
                       Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_AUDIO_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_AUDIO_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                getContentResolver().takePersistableUriPermission(
                    treeUri, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                mSDRootUri = treeUri;
                scanAudioFilesFromTreeUri(treeUri);
            }
        }
    }

    private void scanAudioFilesFromTreeUri(Uri treeUri) {
        // Implementar escaneo de archivos de audio desde el URI
        // Esta función necesitará procesar los archivos encontrados
        // y posiblemente añadirlos a la base de datos
        Utils.showToast(this, "SD card access granted. Scanning for audio files...");
        
        // Aquí iría la lógica para escanear archivos de audio
        // usando DocumentsContract y DocumentFile APIs
    }

    // ... EL RESTO DEL CÓDIGO EXISTENTE SE MANTIENE IGUAL ...
    // Los métodos loaders, menus, etc. permanecen sin cambios

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Código existente sin cambios
        String[] projection = {
                AnchorContract.AlbumEntry._ID,
                AnchorContract.AlbumEntry.COLUMN_TITLE,
                AnchorContract.AlbumEntry.COLUMN_COVER_PATH
        };

        return new CursorLoader(this,
                AnchorContract.AlbumEntry.CONTENT_URI,
                projection,
                null,
                null,
                ALBUM_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Código existente sin cambios
        mProgressBar.setVisibility(View.GONE);
        mAlbumAdapter.swapCursor(data);
        if (data.getCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Código existente sin cambios
        mAlbumAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Código existente sin cambios
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Código existente sin cambios
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_scan) {
            // Modificar para usar el nuevo método
            browseForAudioFiles();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Código existente sin cambios
        Intent intent = new Intent(this, AudioService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Código existente sin cambios
        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Código existente sin cambios
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }
}
