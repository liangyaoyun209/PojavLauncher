package net.kdt.pojavlaunch;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.support.design.widget.*;
import android.support.design.widget.VerticalTabLayout.*;
import android.support.v4.view.*;
import android.support.v7.app.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import com.google.gson.*;
import com.kdt.filerapi.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.mcfragments.*;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.util.*;
import net.kdt.pojavlaunch.value.*;
import org.apache.commons.io.*;
import org.lwjgl.glfw.*;

import android.support.v7.app.AlertDialog;
//import android.support.v7.view.menu.*;
//import net.zhuoweizhang.boardwalk.downloader.*;

public class PojavLauncherActivity extends AppCompatActivity
{
    //private FragmentTabHost mTabHost;
    private LinearLayout fullTab, leftTab;
    /*
     private PojavLauncherViewPager viewPager;
     private VerticalTabLayout tabLayout;
     */

    private ViewPager viewPager;
    private VerticalTabLayout tabLayout;

    private TextView tvVersion, tvUsernameView;
    private Spinner accountSelector, versionSelector;
    private String[] availableVersions = Tools.versionList;
    private MCProfile.Builder profile;
    private String profilePath = null;
    private CrashFragment crashView;
    private ConsoleFragment consoleView;
    private ViewPagerAdapter viewPageAdapter;

    private ProgressBar launchProgress;
    private TextView launchTextStatus;
    private Button switchUsrBtn, logoutBtn; // MineButtons
    private ViewGroup leftView, rightView;
    private Button playButton;

    private Gson gson;

    private JMinecraftVersionList versionList;
    private static volatile boolean isAssetsProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        gson = new Gson();

        viewInit();

        Tools.setFullscreen(this);

        if (BuildConfig.DEBUG)
            Toast.makeText(this, "Launcher process id: " + android.os.Process.myPid(), Toast.LENGTH_LONG).show();
    }
    // DEBUG
    //new android.support.design.widget.NavigationView(this);

    private void viewInit() {
        setContentView(R.layout.launcher_main_v3);
        // setContentView(R.layout.launcher_main);

        leftTab = findViewById(R.id.launchermain_layout_leftmenu);
        leftTab.setLayoutParams(new LinearLayout.LayoutParams(
            CallbackBridge.windowWidth / 4,
            LinearLayout.LayoutParams.MATCH_PARENT));
        
        fullTab = findViewById(R.id.launchermain_layout_viewpager);
        tabLayout = findViewById(R.id.launchermainTabLayout);
        viewPager = findViewById(R.id.launchermainTabPager);

        consoleView = new ConsoleFragment();
        crashView = new CrashFragment();

        viewPageAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPageAdapter.addFragment(new LauncherFragment(), R.drawable.ic_menu_news, getString(R.string.mcl_tab_news));
        viewPageAdapter.addFragment(consoleView, R.drawable.ic_menu_java, getString(R.string.mcl_tab_console));
        viewPageAdapter.addFragment(crashView, 0, getString(R.string.mcl_tab_crash));
        viewPageAdapter.addFragment(new LauncherPreferenceFragment(), R.drawable.ic_menu_settings, getString(R.string.mcl_option_settings));
        
        viewPager.setAdapter(viewPageAdapter);
        // tabLayout.setTabMode(VerticalTabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setLastTabAsBottom();

        tvUsernameView = (TextView) findViewById(R.id.launcherMainUsernameView);
        tvVersion = (TextView) findViewById(R.id.launcherMainVersionView);

        try {
            profilePath = PojavProfile.getCurrentProfilePath(this);
            profile = PojavProfile.getCurrentProfileContent(this);

            tvUsernameView.setText(profile.getUsername());
        } catch(Exception e) {
            //Tools.throwError(this, e);
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.toast_login_error, e.getMessage()), Toast.LENGTH_LONG).show();
            finish();
        }

        File logFile = new File(Tools.MAIN_PATH, "latestlog.txt");
        if (logFile.exists() && logFile.length() < 20480) {
            String errMsg = "Error occurred during initialization of ";
            try {
                String logContent = Tools.read(logFile.getAbsolutePath());
                if (logContent.contains(errMsg + "VM") && 
                    logContent.contains("Could not reserve enough space for")) {
                    OutOfMemoryError ex = new OutOfMemoryError("Java error: " + logContent);
                    ex.setStackTrace(null);
                    Tools.showError(PojavLauncherActivity.this, ex);

                    // Do it so dialog will not shown for second time
                    Tools.write(logFile.getAbsolutePath(), logContent.replace(errMsg + "VM", errMsg + "JVM"));
                }
            } catch (Throwable th) {
                System.err.println("Could not detect java crash");
                th.printStackTrace();
            }
        }

        //showProfileInfo();

        final String[] accountList = new File(Tools.mpProfiles).list();
        
        ArrayAdapter<String> adapterAcc = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, accountList);
        adapterAcc.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        accountSelector = (Spinner) findViewById(R.id.launchermain_spinner_account);
        accountSelector.setAdapter(adapterAcc);
        accountSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4) {
                PojavProfile.setCurrentProfile(PojavLauncherActivity.this, accountList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p1) {
                // TODO: Implement this method
            }
        });
        
        List<String> versions = new ArrayList<String>();
        final File fVers = new File(Tools.versnDir);

        try {
            if (fVers.listFiles().length < 1) {
                throw new Exception(getString(R.string.error_no_version));
            }

            for (File fVer : fVers.listFiles()) {
                if (fVer.isDirectory())
                    versions.add(fVer.getName());
            }
        } catch (Exception e) {
            versions.add(getString(R.string.global_error) + ":");
            versions.add(e.getMessage());

        } finally {
            availableVersions = versions.toArray(new String[0]);
        }

        //availableVersions;

        ArrayAdapter<String> adapterVer = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, availableVersions);
        adapterVer.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        versionSelector = (Spinner) findViewById(R.id.launchermain_spinner_version);
        versionSelector.setAdapter(adapterVer);

        launchProgress = (ProgressBar) findViewById(R.id.progressDownloadBar);
        launchTextStatus = (TextView) findViewById(R.id.progressDownloadText);
        LinearLayout exitLayout = (LinearLayout) findViewById(R.id.launcherMainExitbtns);
        switchUsrBtn = (Button) exitLayout.getChildAt(0);
        logoutBtn = (Button) exitLayout.getChildAt(1);

        leftView = (LinearLayout) findViewById(R.id.launcherMainLeftLayout);
        playButton = (Button) findViewById(R.id.launcherMainPlayButton);
        rightView = (ViewGroup) findViewById(R.id.launcherMainRightLayout);

        statusIsLaunching(false);
    }

    public class RefreshVersionListTask extends AsyncTask<Void, Void, ArrayList<String>>{

        @Override
        protected ArrayList<String> doInBackground(Void[] p1)
        {
            try{
                versionList = gson.fromJson(DownloadUtils.downloadString("https://launchermeta.mojang.com/mc/game/version_manifest.json"), JMinecraftVersionList.class);
                ArrayList<String> versionStringList = filter(versionList.versions, new File(Tools.versnDir).listFiles());

                return versionStringList;
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result)
        {
            super.onPostExecute(result);

            final PopupMenu popup = new PopupMenu(PojavLauncherActivity.this, versionSelector);  
            popup.getMenuInflater().inflate(R.menu.menu_versionopt, popup.getMenu());  

            if(result != null && result.size() > 0) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(PojavLauncherActivity.this, android.R.layout.simple_spinner_item, result);
                adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
                versionSelector.setAdapter(adapter);
                versionSelector.setSelection(selectAt(result.toArray(new String[0]), profile.getVersion()));
            } else {
                versionSelector.setSelection(selectAt(availableVersions, profile.getVersion()));
            }
            versionSelector.setOnItemSelectedListener(new OnItemSelectedListener(){

                    @Override
                    public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4)
                    {
                        String version = p1.getItemAtPosition(p3).toString();
                        profile.setVersion(version);

                        PojavProfile.setCurrentProfile(PojavLauncherActivity.this, profile);
                        if (PojavProfile.isFileType(PojavLauncherActivity.this)) {
                            PojavProfile.setCurrentProfile(PojavLauncherActivity.this, MCProfile.build(profile));
                        }

                        tvVersion.setText(getString(R.string.mcl_version_msg, version));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p1)
                    {
                        // TODO: Implement this method
                    }
                });
            versionSelector.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
                    @Override
                    public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4)
                    {
                        // Implement copy, remove, reinstall,...
                        popup.show();
                        return true;
                    }
                });

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {  
                    public boolean onMenuItemClick(MenuItem item) {  
                        return true;  
                    }  
                });  

            tvVersion.setText(getString(R.string.mcl_version_msg) + versionSelector.getSelectedItem());
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.updateWindowSize(this);
    }

    private float updateWidthHeight() {
        float leftRightWidth = (float) CallbackBridge.windowWidth / 100f * 32f;
        float playButtonWidth = CallbackBridge.windowWidth - leftRightWidth * 2f;
        LinearLayout.LayoutParams leftRightParams = new LinearLayout.LayoutParams((int) leftRightWidth, (int) Tools.dpToPx(this, CallbackBridge.windowHeight / 9));
        LinearLayout.LayoutParams playButtonParams = new LinearLayout.LayoutParams((int) playButtonWidth, (int) Tools.dpToPx(this, CallbackBridge.windowHeight / 9));
        leftView.setLayoutParams(leftRightParams);
        rightView.setLayoutParams(leftRightParams);
        playButton.setLayoutParams(playButtonParams);

        return leftRightWidth;
    }

    private JMinecraftVersionList.Version findVersion(String version) {
        if (versionList != null) {
            for (JMinecraftVersionList.Version valueVer: versionList.versions) {
                if (valueVer.id.equals(version)) {
                    return valueVer;
                }
            }
        }

        // Custom version, inherits from base.
        return Tools.getVersionInfo(version);
    }

    private ArrayList<String> filter(JMinecraftVersionList.Version[] list1, File[] list2) {
        ArrayList<String> output = new ArrayList<String>();

        for (JMinecraftVersionList.Version value1: list1) {
            if ((value1.type.equals("release") && LauncherPreferences.PREF_VERTYPE_RELEASE) ||
                (value1.type.equals("snapshot") && LauncherPreferences.PREF_VERTYPE_SNAPSHOT) ||
                (value1.type.equals("old_alpha") && LauncherPreferences.PREF_VERTYPE_OLDALPHA) ||
                (value1.type.equals("old_beta") && LauncherPreferences.PREF_VERTYPE_OLDBETA)) {
                output.add(value1.id);
            }
        }

        for (File value2: list2) {
            if (!output.contains(value2.getName())) {
                output.add(value2.getName());
            }
        }

        return output;
    }

    public void mcaccSwitchUser(View view)
    {
        showProfileInfo();
    }

    public void mcaccLogout(View view)
    {
        //PojavProfile.reset();
        finish();
    }

    private void showProfileInfo()
    {
        /*
         new AlertDialog.Builder(this)
         .setTitle("Info player")
         .setMessage(
         "AccessToken=" + profile.getAccessToken() + "\n" +
         "ClientID=" + profile.getClientID() + "\n" +
         "ProfileID=" + profile.getProfileID() + "\n" +
         "Username=" + profile.getUsername() + "\n" +
         "Version=" + profile.getVersion()
         ).show();
         */
    }

    private void selectTabPage(int pageIndex){
        if (tabLayout.getSelectedTabPosition() != pageIndex) {
            tabLayout.setScrollPosition(pageIndex,0f,true);
            viewPager.setCurrentItem(pageIndex);
        }
    }

    @Override
    protected void onResumeFragments()
    {
        super.onResumeFragments();
        new RefreshVersionListTask().execute();

        try{
            final ProgressDialog barrier = new ProgressDialog(this);
            barrier.setMessage("Waiting");
            barrier.setProgressStyle(barrier.STYLE_SPINNER);
            barrier.setCancelable(false);
            barrier.show();

            new Thread(new Runnable(){

                    @Override
                    public void run()
                    {
                        while (consoleView == null) {
                            try {
                                Thread.sleep(20);
                            } catch (Throwable th) {}
                        }

                        try {
                            Thread.sleep(100);
                        } catch (Throwable th) {}

                        runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    try {
                                        consoleView.putLog("");
                                        barrier.dismiss();
                                    } catch (Throwable th) {
                                        startActivity(getIntent());
                                        finish();
                                    }
                                }
                            });
                    }
                }).start();

            File lastCrashFile = Tools.lastFileModified(Tools.crashPath);
            if(CrashFragment.isNewCrash(lastCrashFile) || !crashView.getLastCrash().isEmpty()){
                crashView.resetCrashLog = false;
                selectTabPage(2);
            } else throw new Exception();
        } catch(Throwable e){
            selectTabPage(tabLayout.getSelectedTabPosition());
        }
    }

    public int selectAt(String[] strArr, String select)
    {
        int count = 0;
        for(String str : strArr){
            if(str.equals(select)){
                return count;
            }
            else{
                count++;
            }
        }
        return -1;
    }

    @Override
    protected void onResume(){
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }

    private boolean canBack = false;
    private void statusIsLaunching(boolean isLaunching) {
        // As preference fragment put to tab, changes without notice, so need re-load pref
        if (isLaunching) LauncherPreferences.loadPreferences();
        
        LinearLayout.LayoutParams reparam = new LinearLayout.LayoutParams((int) updateWidthHeight(), LinearLayout.LayoutParams.WRAP_CONTENT);
        ViewGroup.MarginLayoutParams lmainTabParam = (ViewGroup.MarginLayoutParams) fullTab.getLayoutParams();
        int launchVisibility = isLaunching ? View.VISIBLE : View.GONE;
        launchProgress.setVisibility(launchVisibility);
        launchTextStatus.setVisibility(launchVisibility);
        lmainTabParam.bottomMargin = reparam.height;
        leftView.setLayoutParams(reparam);

        switchUsrBtn.setEnabled(!isLaunching);
        logoutBtn.setEnabled(!isLaunching);
        versionSelector.setEnabled(!isLaunching);
        canBack = !isLaunching;
    }
    @Override
    public void onBackPressed()
    {
        if (canBack) {
            super.onBackPressed();
        }
    }

    // Catching touch exception
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return super.onTouchEvent(event);
        } catch (Throwable th) {
            Tools.showError(this, th);
            return false;
        }
    }

    private GameRunnerTask mTask;

    public void launchGame(View v)
    {
        if (!canBack && isAssetsProcessing) {
            isAssetsProcessing = false;
            statusIsLaunching(false);
        } else if (canBack) {
            v.setEnabled(false);
            mTask = new GameRunnerTask();
            mTask.execute(profile.getVersion());
            crashView.resetCrashLog = true;
        }
    }

    public class GameRunnerTask extends AsyncTask<String, String, Throwable>
    {
        private boolean launchWithError = false;

        @Override
        protected void onPreExecute() {
            launchProgress.setMax(1);
            statusIsLaunching(true);
        }

        private JMinecraftVersionList.Version verInfo;
        @Override
        protected Throwable doInBackground(final String[] p1) {
            Throwable throwable = null;
            try {
                final String downVName = "/" + p1[0] + "/" + p1[0];

                //Downloading libraries
                String minecraftMainJar = Tools.versnDir + downVName + ".jar";
                JAssets assets = null;
                try {
                    //com.pojavdx.dx.mod.Main.debug = true;

                    String verJsonDir = Tools.versnDir + downVName + ".json";

                    verInfo = findVersion(p1[0]);

                    if (verInfo.url != null && !new File(verJsonDir).exists()) {
                        publishProgress("1", "Downloading " + p1[0] + " configuration...");
                        Tools.downloadFile(
                            verInfo.url,
                            verJsonDir
                        );
                    }

                    verInfo = Tools.getVersionInfo(p1[0]);
                    assets = downloadIndex(verInfo.assets, new File(Tools.ASSETS_PATH, "indexes/" + verInfo.assets + ".json"));

                    File outLib;
                    String libPathURL;

                    setMax(verInfo.libraries.length + 4 + assets.objects.size());
                    for (final DependentLibrary libItem : verInfo.libraries) {

                        if (// libItem.name.startsWith("com.google.code.gson:gson") ||
                        // libItem.name.startsWith("com.mojang:realms") ||
                            libItem.name.startsWith("net.java.jinput") ||
                        // libItem.name.startsWith("net.minecraft.launchwrapper") ||

                        // FIXME lib below!
                        // libItem.name.startsWith("optifine:launchwrapper-of") ||

                        // libItem.name.startsWith("org.lwjgl.lwjgl:lwjgl") ||
                            libItem.name.startsWith("org.lwjgl")
                        // libItem.name.startsWith("tv.twitch")
                            ) { // Black list
                            publishProgress("1", "Ignored " + libItem.name);
                            //Thread.sleep(100);
                        } else {

                            String[] libInfo = libItem.name.split(":");
                            String libArtifact = Tools.artifactToPath(libInfo[0], libInfo[1], libInfo[2]);
                            outLib = new File(Tools.libraries + "/" + libArtifact);
                            outLib.getParentFile().mkdirs();

                            if (!outLib.exists()) {
                                publishProgress("1", getString(R.string.mcl_launch_download_lib, libItem.name));

                                boolean skipIfFailed = false;

                                if (libItem.downloads == null || libItem.downloads.artifact == null) {
                                    MinecraftLibraryArtifact artifact = new MinecraftLibraryArtifact();
                                    artifact.url = (libItem.url == null ? "https://libraries.minecraft.net/" : libItem.url) + libArtifact;
                                    libItem.downloads = new DependentLibrary.LibraryDownloads(artifact);

                                    skipIfFailed = true;
                                }
                                try {
                                    libPathURL = libItem.downloads.artifact.url;
                                    Tools.downloadFile(
                                        libPathURL,
                                        outLib.getAbsolutePath()
                                    );
                                } catch (Throwable th) {
                                    if (!skipIfFailed) {
                                        throw th;
                                    } else {
                                        th.printStackTrace();
                                        publishProgress("0", th.getMessage());
                                    }
                                }
                            }
                        }
                    }

                    publishProgress("1", getString(R.string.mcl_launch_download_client, p1[0]));
                    File minecraftMainFile = new File(minecraftMainJar);
                    if (!minecraftMainFile.exists() || minecraftMainFile.length() == 0l) {
                        try {
                            Tools.downloadFile(
                                verInfo.downloads.values().toArray(new MinecraftClientInfo[0])[0].url,
                                minecraftMainJar
                            );
                        } catch (Throwable th) {
                            if (verInfo.inheritsFrom != null) {
                                minecraftMainFile.delete();
                                IOUtils.copy(new FileInputStream(new File(Tools.versnDir, verInfo.inheritsFrom + "/" + verInfo.inheritsFrom + ".jar")), new FileOutputStream(minecraftMainFile));
                            } else {
                                throw th;
                            }
                        }
                    }
                } catch (Throwable e) {
                    launchWithError = true;
                    throw e;
                }

                publishProgress("1", getString(R.string.mcl_launch_cleancache));
                // new File(inputPath).delete();

                for (File f : new File(Tools.versnDir).listFiles()) {
                    if(f.getName().endsWith(".part")) {
                        Log.d(Tools.APP_NAME, "Cleaning cache: " + f);
                        f.delete();
                    }
                }

                isAssetsProcessing = true;
                playButton.post(new Runnable(){

                        @Override
                        public void run()
                        {
                            playButton.setText("Skip");
                            playButton.setEnabled(true);
                        }
                    });
                publishProgress("1", getString(R.string.mcl_launch_download_assets));
                try {
                    downloadAssets(assets, verInfo.assets, new File(Tools.ASSETS_PATH));
                } catch (Exception e) {
                    e.printStackTrace();

                    // Ignore it
                    launchWithError = false;
                } finally {
                    isAssetsProcessing = false;
                }
            } catch (Throwable th){
                throwable = th;
            } finally {
                return throwable;
            }
        }
        private int addProgress = 0; // 34

        public void zeroProgress()
        {
            addProgress = 0;
        }

        public void setMax(final int value)
        {
            launchProgress.post(new Runnable(){

                    @Override
                    public void run()
                    {
                        launchProgress.setMax(value);
                    }
                });
        }

        @Override
        protected void onProgressUpdate(String... p1)
        {
            int addedProg = Integer.parseInt(p1[0]);
            if (addedProg != -1) {
                addProgress = addProgress + addedProg;
                launchProgress.setProgress(addProgress);

                launchTextStatus.setText(p1[1]);
            }

            if (p1.length < 3) consoleView.putLog(p1[1] + (p1.length < 3 ? "\n" : ""));
        }

        @Override
        protected void onPostExecute(Throwable p1)
        {
            playButton.setText("Play");
            playButton.setEnabled(true);
            launchProgress.setMax(100);
            launchProgress.setProgress(0);
            statusIsLaunching(false);
            if(p1 != null) {
                p1.printStackTrace();
                Tools.showError(PojavLauncherActivity.this, p1);
            }
            if(!launchWithError) {
                crashView.setLastCrash("");

                try {
                    /*
                     List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
                     jvmArgs.add("-Xms128M");
                     jvmArgs.add("-Xmx1G");
                     */
                    Intent mainIntent = new Intent(PojavLauncherActivity.this, MainActivity.class);
                    // mainIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    if (LauncherPreferences.PREF_FREEFORM) {
                        DisplayMetrics dm = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(dm);

                        ActivityOptions options = (ActivityOptions) ActivityOptions.class.getMethod("makeBasic").invoke(null);
                        Rect freeformRect = new Rect(0, 0, dm.widthPixels / 2, dm.heightPixels / 2);
                        options.getClass().getDeclaredMethod("setLaunchBounds", Rect.class).invoke(options, freeformRect);
                        startActivity(mainIntent, options.toBundle());
                    } else {
                        startActivity(mainIntent);
                    }
                }
                catch (Throwable e) {
                    Tools.showError(PojavLauncherActivity.this, e);
                }

                /*
                 FloatingIntent maini = new FloatingIntent(PojavLauncherActivity.this, MainActivity.class);
                 maini.startFloatingActivity();
                 */
            }

            mTask = null;
        }

        private Gson gsonss = gson;
        public static final String MINECRAFT_RES = "http://resources.download.minecraft.net/";

        public JAssets downloadIndex(String versionName, File output) throws Exception {
            String versionJson = DownloadUtils.downloadString(verInfo.assetIndex != null ? verInfo.assetIndex.url : "http://s3.amazonaws.com/Minecraft.Download/indexes/" + versionName + ".json");
            JAssets version = gsonss.fromJson(versionJson, JAssets.class);
            output.getParentFile().mkdirs();
            Tools.write(output.getAbsolutePath(), versionJson.getBytes(Charset.forName("UTF-8")));
            return version;
        }

        public void downloadAsset(JAssetInfo asset, File objectsDir) throws IOException, Throwable {
            String assetPath = asset.hash.substring(0, 2) + "/" + asset.hash;
            File outFile = new File(objectsDir, assetPath);
            if (!outFile.exists()) {
                DownloadUtils.downloadFile(MINECRAFT_RES + assetPath, outFile);
            }
        }

        public void downloadAssets(JAssets assets, String assetsVersion, File outputDir) throws IOException, Throwable {
            File hasDownloadedFile = new File(outputDir, "downloaded/" + assetsVersion + ".downloaded");
            if (!hasDownloadedFile.exists()) {
                System.out.println("Assets begin time: " + System.currentTimeMillis());
                Map<String, JAssetInfo> assetsObjects = assets.objects;
                launchProgress.setMax(assetsObjects.size());
                zeroProgress();
                File objectsDir = new File(outputDir, "objects");
                int downloadedSs = 0;
                for (JAssetInfo asset : assetsObjects.values()) {
                    if (!isAssetsProcessing) {
                        return;
                    }

                    downloadAsset(asset, objectsDir);
                    publishProgress("1", getString(R.string.mcl_launch_downloading, assetsObjects.keySet().toArray(new String[0])[downloadedSs]));
                    downloadedSs++;
                }
                hasDownloadedFile.getParentFile().mkdirs();
                hasDownloadedFile.createNewFile();
                System.out.println("Assets end time: " + System.currentTimeMillis());
            }
        }
    }
    
    public void launcherMenu(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.mcl_options);
        builder.setItems(R.array.mcl_options, new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2)
                {
                    switch (p2) {
                        case 0: // Mod installer
                            installMod(false);
                            break;
                        case 1: // Mod installer with java args 
                            installMod(true);
                            break;
                        case 2: // Custom controls
                            if (Tools.enableDevFeatures) {
                                startActivity(new Intent(PojavLauncherActivity.this, CustomControlsActivity.class));
                            }
                            break;
                        case 3: // Settings
                            startActivity(new Intent(PojavLauncherActivity.this, LauncherPreferenceActivity.class));
                            break;
                        case 4: { // About
                                final AlertDialog.Builder aboutB = new AlertDialog.Builder(PojavLauncherActivity.this);
                                aboutB.setTitle(R.string.mcl_option_about);
                                try
                                {
                                    aboutB.setMessage(Html.fromHtml(String.format(Tools.read(getAssets().open("about_en.txt")),
                                                                                  Tools.APP_NAME,
                                                                                  Tools.usingVerName,
                                                                                  "3.2.3")
                                                                    ));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                aboutB.setPositiveButton(android.R.string.ok, null);
                                aboutB.show();
                            } break;
                    }
                }
            });
        builder.show();
    }

    private void installMod(boolean customJavaArgs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alerttitle_installmod);
        builder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog;
        if (customJavaArgs) {
            final EditText edit = new EditText(this);
            edit.setSingleLine();
            edit.setHint("-jar/-cp /path/to/file.jar ...");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface di, int i) {
                        Intent intent = new Intent(PojavLauncherActivity.this, InstallModActivity.class);
                        intent.putExtra("javaArgs", edit.getText().toString());
                        startActivity(intent);
                    }
                });
            dialog = builder.create();
            dialog.setView(edit);
        } else {
            dialog = builder.create();
            FileListView flv = new FileListView(dialog);
            flv.setFileSelectedListener(new FileSelectedListener(){

                    @Override
                    public void onFileSelected(File file, String path) {
                        if (file.getName().endsWith(".jar")) {
                            Intent intent = new Intent(PojavLauncherActivity.this, InstallModActivity.class);
                            intent.putExtra("modFile", file);
                            startActivity(intent);
                            dialog.dismiss();
                        }
                    }
                });
            dialog.setView(flv);
        }
        dialog.show();
    }
}