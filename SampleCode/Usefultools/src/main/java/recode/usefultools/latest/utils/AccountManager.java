/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.authlib.minecraft.UserApiService
 *  imgui.ImGui
 *  imgui.type.ImString
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.User
 *  net.minecraft.client.multiplayer.ProfileKeyPairManager
 */
package recode.usefultools.latest.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.UserApiService;
import imgui.ImGui;
import imgui.type.ImString;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import recode.usefultools.latest.mixin.MinecraftAccessor;

public class AccountManager {
    public final static AccountManager INSTANCE = new AccountManager();
    private final File accountsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<AltAccount> accounts = new ArrayList<AltAccount>();
    public boolean showScreen = false;
    private boolean isPlusExpanded = false;
    private final ImString offlineNameInput = new ImString(64);
    private final ImString sessionTokenInput = new ImString(4096);
    private final ImString refreshTokenInput = new ImString(4096);
    public String lastLoginError = "";
    private boolean pxsetmode = false;
    private float customX = 0.0f;
    private float customY = 0.0f;
    private String anchorX = "right";
    private String anchorY = "down";

    private AccountManager() {
        Minecraft mc = Minecraft.getInstance();
        this.accountsFile = new File(mc.gameDirectory, "useful-tools-custom-config/accounts.json");
        this.loadConfig();
        this.loadAccounts();
    }

    public void loadAccounts() {
        if (!this.accountsFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(this.accountsFile);){
            this.accounts.clear();
            JsonArray array = JsonParser.parseReader((Reader)reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                this.accounts.add(new AltAccount(obj.get("name").getAsString(), obj.get("uuid").getAsString(), obj.get("token").getAsString(), obj.get("type").getAsString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAccounts() {
        try {
            if (!this.accountsFile.getParentFile().exists()) {
                this.accountsFile.getParentFile().mkdirs();
            }
            JsonArray array = new JsonArray();
            for (AltAccount acc : this.accounts) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", acc.name);
                obj.addProperty("uuid", acc.uuid);
                obj.addProperty("token", acc.token);
                obj.addProperty("type", acc.type);
                array.add((JsonElement)obj);
            }
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(this.accountsFile)));){
                writer.print(this.gson.toJson((JsonElement)array));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        File cfgFile = new File(Minecraft.getInstance().gameDirectory, "useful-tools-custom-config/account_manager_layout.json");
        if (!cfgFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(cfgFile);){
            JsonObject obj = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            if (obj.has("pxsetmode")) {
                this.pxsetmode = obj.get("pxsetmode").getAsBoolean();
            }
            if (obj.has("customX")) {
                this.customX = obj.get("customX").getAsFloat();
            }
            if (obj.has("customY")) {
                this.customY = obj.get("customY").getAsFloat();
            }
            if (obj.has("anchorX")) {
                this.anchorX = obj.get("anchorX").getAsString();
            }
            if (obj.has("anchorY")) {
                this.anchorY = obj.get("anchorY").getAsString();
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    private UUID getSafeUUID(String uuidStr) {
        try {
            return UUID.fromString(uuidStr);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }

    public boolean login(AltAccount account) {
        Minecraft mc = Minecraft.getInstance();
        this.lastLoginError = "";
        if (account.type.equalsIgnoreCase("Microsoft") && !account.token.startsWith("eyJ")) {
            this.lastLoginError = "Refreshing Microsoft Token...";
            CompletableFuture.runAsync(() -> {
                String newAccess = this.refreshMicrosoftToken(account.token);
                if (newAccess != null) {
                    account.token = newAccess;
                    this.saveAccounts();
                    mc.execute(() -> this.login(account));
                } else {
                    this.lastLoginError = "Failed to refresh Microsoft Token.";
                }
            });
            return false;
        }
        try {
            User newUser = new User(account.name, this.getSafeUUID(account.uuid), account.token, Optional.empty(), Optional.empty());
            ((MinecraftAccessor)mc).setUser(newUser);
            UserApiService userApiService = ((MinecraftAccessor)mc).getUserApiService();
            ProfileKeyPairManager newKeyManager = ProfileKeyPairManager.create((UserApiService)userApiService, (User)newUser, (Path)mc.gameDirectory.toPath());
            ((MinecraftAccessor)mc).setProfileKeyPairManager(newKeyManager);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            this.lastLoginError = t.getClass().getSimpleName() + ": " + t.getMessage();
            return false;
        }
    }

    private String refreshMicrosoftToken(String refreshToken) {
        try {
            JsonObject mcRes;
            JsonObject xstsRes;
            JsonObject xblRes;
            JsonObject msJson;
            URL url = new URL("https://login.live.com/oauth20_token.srf");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            String postData = "client_id=00000000402b5328&grant_type=refresh_token&refresh_token=" + refreshToken;
            try (OutputStream os = conn.getOutputStream();){
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));){
                msJson = JsonParser.parseReader((Reader)br).getAsJsonObject();
            }
            String msAccessToken = msJson.get("access_token").getAsString();
            URL xblUrl = new URL("https://user.auth.xboxlive.com/user/authenticate");
            HttpURLConnection xblConn = (HttpURLConnection)xblUrl.openConnection();
            xblConn.setRequestMethod("POST");
            xblConn.setRequestProperty("Content-Type", "application/json");
            xblConn.setRequestProperty("Accept", "application/json");
            xblConn.setDoOutput(true);
            JsonObject xblReq = new JsonObject();
            JsonObject xblProps = new JsonObject();
            xblProps.addProperty("AuthMethod", "RPS");
            xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
            xblProps.addProperty("RpsTicket", "d=" + msAccessToken);
            xblReq.add("Properties", (JsonElement)xblProps);
            xblReq.addProperty("RelyingParty", "http://auth.xboxlive.com");
            xblReq.addProperty("TokenType", "JWT");
            try (OutputStream os = xblConn.getOutputStream();){
                os.write(this.gson.toJson((JsonElement)xblReq).getBytes(StandardCharsets.UTF_8));
            }
            if (xblConn.getResponseCode() != 200) {
                return null;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(xblConn.getInputStream(), StandardCharsets.UTF_8));){
                xblRes = JsonParser.parseReader((Reader)br).getAsJsonObject();
            }
            String xblToken = xblRes.get("Token").getAsString();
            String uhs = xblRes.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
            URL xstsUrl = new URL("https://xsts.auth.xboxlive.com/xsts/authorize");
            HttpURLConnection xstsConn = (HttpURLConnection)xstsUrl.openConnection();
            xstsConn.setRequestMethod("POST");
            xstsConn.setRequestProperty("Content-Type", "application/json");
            xstsConn.setDoOutput(true);
            JsonObject xstsReq = new JsonObject();
            JsonObject xstsProps = new JsonObject();
            xstsProps.addProperty("SandboxId", "RETAIL");
            JsonArray userTokens = new JsonArray();
            userTokens.add(xblToken);
            xstsProps.add("UserTokens", (JsonElement)userTokens);
            xstsReq.add("Properties", (JsonElement)xstsProps);
            xstsReq.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            xstsReq.addProperty("TokenType", "JWT");
            try (OutputStream os = xstsConn.getOutputStream();){
                os.write(this.gson.toJson((JsonElement)xstsReq).getBytes(StandardCharsets.UTF_8));
            }
            if (xstsConn.getResponseCode() != 200) {
                return null;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(xstsConn.getInputStream(), StandardCharsets.UTF_8));){
                xstsRes = JsonParser.parseReader((Reader)br).getAsJsonObject();
            }
            String xstsToken = xstsRes.get("Token").getAsString();
            URL mcUrl = new URL("https://api.minecraftservices.com/authentication/login_with_xbox");
            HttpURLConnection mcConn = (HttpURLConnection)mcUrl.openConnection();
            mcConn.setRequestMethod("POST");
            mcConn.setRequestProperty("Content-Type", "application/json");
            mcConn.setDoOutput(true);
            JsonObject mcReq = new JsonObject();
            mcReq.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
            try (OutputStream os = mcConn.getOutputStream();){
                os.write(this.gson.toJson((JsonElement)mcReq).getBytes(StandardCharsets.UTF_8));
            }
            if (mcConn.getResponseCode() != 200) {
                return null;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(mcConn.getInputStream(), StandardCharsets.UTF_8));){
                mcRes = JsonParser.parseReader((Reader)br).getAsJsonObject();
            }
            return mcRes.get("access_token").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void fetchAndAddSessionAccount(String token, String type) {
        CompletableFuture.runAsync(() -> {
            block11: {
                try {
                    URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    if (conn.getResponseCode() == 200) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));){
                            JsonObject profileObj = JsonParser.parseReader((Reader)reader).getAsJsonObject();
                            String resolvedName = profileObj.get("name").getAsString();
                            String resolvedUUID = profileObj.get("id").getAsString();
                            String formattedUUID = resolvedUUID.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
                            List<AltAccount> list = this.accounts;
                            synchronized (list) {
                                this.accounts.add(new AltAccount(resolvedName, formattedUUID, token, type));
                                this.saveAccounts();
                                break block11;
                            }
                        }
                    }
                    this.lastLoginError = "Mojang API Error: HTTP " + conn.getResponseCode();
                } catch (Exception e) {
                    e.printStackTrace();
                    this.lastLoginError = "Connection Failed: " + e.getMessage();
                }
            }
        });
    }

    public void draw() {
        boolean isTargetScreen;
        Minecraft mc = Minecraft.getInstance();
        boolean bl = isTargetScreen = mc.screen != null && (mc.screen.getClass().getSimpleName().equals("TitleScreen") || mc.screen.getClass().getSimpleName().equals("JoinMultiplayerScreen"));
        if (!isTargetScreen) {
            this.showScreen = false;
            return;
        }
        if (this.showScreen) {
            this.drawFullscreenManager();
        } else {
            this.drawLaunchButton();
        }
    }

    private void drawLaunchButton() {
        float posY;
        float posX;
        float sw = ImGui.getIO().getDisplaySizeX();
        float sh = ImGui.getIO().getDisplaySizeY();
        float btnW = 150.0f;
        float btnH = 30.0f;
        if (this.pxsetmode) {
            posX = sw / 2.0f - btnW / 2.0f + this.customX;
            posY = sh / 2.0f - btnH / 2.0f + this.customY;
        } else {
            posX = this.anchorX.equalsIgnoreCase("left") ? 20.0f : sw - btnW - 20.0f;
            posY = this.anchorY.equalsIgnoreCase("up") ? 20.0f : sh - btnH - 20.0f;
        }
        ImGui.setNextWindowPos((float)posX, (float)posY);
        ImGui.setNextWindowSize((float)btnW, (float)btnH);
        ImGui.setNextWindowBgAlpha(0.0f);
        int flags = 15;
        ImGui.pushStyleVar(2, 0.0f, 0.0f);
        ImGui.pushStyleVar(4, 0.0f);
        ImGui.pushStyleVar(13, 0.0f);
        if (ImGui.begin((String)"AltBtn", (int)flags)) {
            ImGui.pushStyleColor(21, 0.12f, 0.12f, 0.12f, 0.9f);
            ImGui.pushStyleColor(22, 0.12f, 0.12f, 0.12f, 0.9f);
            ImGui.pushStyleColor(23, 0.12f, 0.12f, 0.12f, 0.9f);
            if (ImGui.button((String)"Account Manager", (float)btnW, (float)btnH)) {
                this.showScreen = true;
                this.isPlusExpanded = false;
            }
            ImGui.popStyleColor(3);
        }
        ImGui.end();
        ImGui.popStyleVar(3);
    }

    private void drawFullscreenManager() {
        float sw = ImGui.getIO().getDisplaySizeX();
        float sh = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowPos(0.0f, 0.0f);
        ImGui.setNextWindowSize((float)sw, (float)sh);
        ImGui.setNextWindowBgAlpha(0.6f);
        int flags = 39;
        ImGui.pushStyleVar(4, 0.0f);
        ImGui.pushStyleVar(8, 0.0f);
        ImGui.pushStyleVar(13, 0.0f);
        if (ImGui.begin((String)"AccountManagerFullscreen", (int)flags)) {
            Minecraft mc = Minecraft.getInstance();
            User currentUser = mc.getUser();
            ImGui.beginChild((String)"LeftPanel", (float)(sw * 0.35f), (float)sh, (boolean)false, 128);
            ImGui.dummy(0.0f, 20.0f);
            ImGui.indent(30.0f);
            ImGui.text((String)"CURRENT LOGGED IN");
            ImGui.getWindowDrawList().addRectFilled(ImGui.getCursorScreenPos().x, ImGui.getCursorScreenPos().y, ImGui.getCursorScreenPos().x + 120.0f, ImGui.getCursorScreenPos().y + 120.0f, ImGui.getColorU32(0.12f, 0.12f, 0.12f, 0.5f), 12.0f);
            ImGui.getWindowDrawList().addText(ImGui.getCursorScreenPos().x + 40.0f, ImGui.getCursorScreenPos().y + 50.0f, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.5f), "Avatar");
            ImGui.dummy(0.0f, 140.0f);
            ImGui.dummy(0.0f, 10.0f);
            ImGui.text((String)("Username: " + (currentUser != null ? currentUser.getName() : "Unknown")));
            String acType = "Offline";
            if (currentUser != null) {
                String currentToken = currentUser.getAccessToken();
                if (currentToken != null && currentToken.startsWith("eyJ")) {
                    acType = "Microsoft";
                } else if (currentToken != null && !currentToken.equals("0") && !currentToken.equals("none")) {
                    acType = "Session";
                }
            }
            int typeColor = acType.equals("Microsoft") ? ImGui.getColorU32(0.3f, 0.82f, 0.3f, 1.0f) : (acType.equals("Session") ? ImGui.getColorU32(0.3f, 0.5f, 0.82f, 1.0f) : ImGui.getColorU32(0.6f, 0.6f, 0.6f, 1.0f));
            ImGui.text((String)"Account Type: ");
            ImGui.sameLine();
            ImGui.textColored((int)typeColor, (String)acType);
            if (!this.lastLoginError.isEmpty()) {
                ImGui.dummy(0.0f, 20.0f);
                ImGui.textColored((int)ImGui.getColorU32(0.9f, 0.2f, 0.2f, 1.0f), (String)"STATUS/ERROR:");
                ImGui.textColored((int)ImGui.getColorU32(0.9f, 0.2f, 0.2f, 1.0f), (String)this.lastLoginError);
            }
            ImGui.dummy(0.0f, 50.0f);
            ImGui.pushStyleVar(12, 6.0f);
            ImGui.pushStyleColor(21, 0.45f, 0.15f, 0.15f, 0.7f);
            ImGui.pushStyleColor(22, 0.45f, 0.15f, 0.15f, 0.7f);
            ImGui.pushStyleColor(23, 0.45f, 0.15f, 0.15f, 0.7f);
            if (ImGui.button((String)"BACK", 120.0f, 35.0f)) {
                this.showScreen = false;
            }
            ImGui.popStyleColor(3);
            ImGui.popStyleVar();
            ImGui.unindent(30.0f);
            ImGui.endChild();
            ImGui.sameLine();
            ImGui.beginChild((String)"RightPanel", (float)(sw * 0.65f - 20.0f), (float)sh, (boolean)false, 128);
            ImGui.dummy(0.0f, 20.0f);
            ImGui.text((String)"REGISTERED ACCOUNTS");
            ImGui.dummy(0.0f, 15.0f);
            if (ImGui.beginChild((String)"AccountScroll", (float)(sw * 0.62f), (float)(sh * 0.72f), (boolean)false, 136)) {
                for (int i = 0; i < this.accounts.size(); ++i) {
                    AltAccount acc = this.accounts.get(i);
                    ImGui.pushID((int)i);
                    float startY = ImGui.getCursorScreenPos().y;
                    ImGui.getWindowDrawList().addRectFilled(ImGui.getCursorScreenPos().x, startY, ImGui.getCursorScreenPos().x + sw * 0.58f, startY + 45.0f, ImGui.getColorU32(0.08f, 0.08f, 0.08f, 0.45f), 10.0f);
                    ImGui.alignTextToFramePadding();
                    ImGui.indent(15.0f);
                    ImGui.dummy(0.0f, 10.0f);
                    ImGui.text((String)acc.name);
                    ImGui.sameLine(220.0f);
                    int tagColor = acc.type.equalsIgnoreCase("Microsoft") ? ImGui.getColorU32(0.3f, 0.82f, 0.3f, 1.0f) : (acc.type.equalsIgnoreCase("Session") ? ImGui.getColorU32(0.3f, 0.5f, 0.82f, 1.0f) : ImGui.getColorU32(0.6f, 0.6f, 0.6f, 1.0f));
                    ImGui.textColored((int)tagColor, (String)acc.type);
                    ImGui.pushStyleVar(12, 4.0f);
                    ImGui.sameLine((float)(sw * 0.4f));
                    ImGui.pushStyleColor(21, 0.12f, 0.35f, 0.12f, 0.7f);
                    ImGui.pushStyleColor(22, 0.12f, 0.35f, 0.12f, 0.7f);
                    ImGui.pushStyleColor(23, 0.12f, 0.35f, 0.12f, 0.7f);
                    if (ImGui.button((String)"LOGIN", 70.0f, 25.0f)) {
                        this.login(acc);
                    }
                    ImGui.popStyleColor(3);
                    ImGui.sameLine((float)(sw * 0.48f));
                    ImGui.pushStyleColor(21, 0.35f, 0.12f, 0.12f, 0.7f);
                    ImGui.pushStyleColor(22, 0.35f, 0.12f, 0.12f, 0.7f);
                    ImGui.pushStyleColor(23, 0.35f, 0.12f, 0.12f, 0.7f);
                    if (ImGui.button((String)"DELETE", 70.0f, 25.0f)) {
                        this.accounts.remove(i);
                        this.saveAccounts();
                        ImGui.popID();
                        ImGui.popStyleColor(3);
                        ImGui.popStyleVar();
                        ImGui.unindent(15.0f);
                        break;
                    }
                    ImGui.popStyleColor(3);
                    ImGui.popStyleVar();
                    ImGui.unindent(15.0f);
                    ImGui.dummy(0.0f, 30.0f);
                    ImGui.popID();
                }
                ImGui.dummy(0.0f, 15.0f);
                if (!this.isPlusExpanded) {
                    ImGui.pushStyleVar(12, 20.0f);
                    ImGui.pushStyleColor(21, 0.12f, 0.12f, 0.12f, 0.8f);
                    ImGui.pushStyleColor(22, 0.12f, 0.12f, 0.12f, 0.8f);
                    ImGui.pushStyleColor(23, 0.12f, 0.12f, 0.12f, 0.8f);
                    if (ImGui.button((String)" + ", 40.0f, 40.0f)) {
                        this.isPlusExpanded = true;
                    }
                    ImGui.popStyleColor(3);
                    ImGui.popStyleVar();
                } else {
                    ImGui.pushStyleVar(12, 6.0f);
                    ImGui.text((String)"ADD NEW ACCOUNT:");
                    ImGui.pushStyleColor(21, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(22, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(23, 0.15f, 0.15f, 0.15f, 0.8f);
                    if (ImGui.button((String)"Microsoft (RefreshToken)", 180.0f, 30.0f)) {
                        ImGui.openPopup((String)"RefreshPopup");
                    }
                    ImGui.sameLine();
                    if (ImGui.button((String)"Session Token", 130.0f, 30.0f)) {
                        ImGui.openPopup((String)"SessionPopup");
                    }
                    ImGui.sameLine();
                    if (ImGui.button((String)"Offline (Cracked)", 140.0f, 30.0f)) {
                        ImGui.openPopup((String)"OfflinePopup");
                    }
                    ImGui.sameLine();
                    ImGui.pushStyleColor(21, 0.35f, 0.12f, 0.12f, 0.8f);
                    ImGui.pushStyleColor(22, 0.35f, 0.12f, 0.12f, 0.8f);
                    ImGui.pushStyleColor(23, 0.35f, 0.12f, 0.12f, 0.8f);
                    if (ImGui.button((String)"Cancel", 80.0f, 30.0f)) {
                        this.isPlusExpanded = false;
                    }
                    ImGui.popStyleColor(6);
                    ImGui.popStyleVar();
                }
                if (ImGui.beginPopupModal((String)"OfflinePopup", 64)) {
                    String name;
                    ImGui.text((String)"Enter Offline Nickname:");
                    ImGui.inputText((String)"##offname", (ImString)this.offlineNameInput);
                    ImGui.spacing();
                    ImGui.pushStyleVar(12, 4.0f);
                    ImGui.pushStyleColor(21, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(22, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(23, 0.15f, 0.15f, 0.15f, 0.8f);
                    if (ImGui.button((String)"Add Account", 120.0f, 25.0f) && !(name = this.offlineNameInput.get().trim()).isEmpty()) {
                        UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
                        this.accounts.add(new AltAccount(name, offlineUUID.toString(), "0", "Offline"));
                        this.saveAccounts();
                        this.offlineNameInput.set("");
                        this.isPlusExpanded = false;
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.sameLine();
                    if (ImGui.button((String)"Cancel", 80.0f, 25.0f)) {
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.popStyleColor(3);
                    ImGui.popStyleVar();
                    ImGui.endPopup();
                }
                if (ImGui.beginPopupModal((String)"RefreshPopup", 64)) {
                    String token;
                    String clipboardText;
                    ImGui.text((String)"Enter/Paste Microsoft Refresh Token:");
                    ImGui.inputText((String)"##reftoken", (ImString)this.refreshTokenInput);
                    ImGui.sameLine();
                    ImGui.pushStyleColor(21, 0.2f, 0.2f, 0.2f, 0.8f);
                    ImGui.pushStyleColor(22, 0.2f, 0.2f, 0.2f, 0.8f);
                    ImGui.pushStyleColor(23, 0.2f, 0.2f, 0.2f, 0.8f);
                    if (ImGui.button((String)"PASTE##ref") && !(clipboardText = mc.keyboardHandler.getClipboard().trim()).isEmpty()) {
                        this.refreshTokenInput.set(clipboardText);
                    }
                    ImGui.popStyleColor(3);
                    ImGui.spacing();
                    ImGui.pushStyleVar(12, 4.0f);
                    ImGui.pushStyleColor(21, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(22, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(23, 0.15f, 0.15f, 0.15f, 0.8f);
                    if (ImGui.button((String)"Add Account", 120.0f, 25.0f) && !(token = this.refreshTokenInput.get().trim()).isEmpty()) {
                        this.lastLoginError = "Resolving token and profiles...";
                        CompletableFuture.runAsync(() -> {
                            String access = this.refreshMicrosoftToken(token);
                            if (access != null) {
                                this.fetchAndAddSessionAccount(access, "Microsoft");
                                List<AltAccount> list = this.accounts;
                                synchronized (list) {
                                    for (AltAccount acc : this.accounts) {
                                        if (!acc.token.equals(access)) continue;
                                        acc.token = token;
                                        this.saveAccounts();
                                        break;
                                    }
                                }
                            } else {
                                this.lastLoginError = "Invalid Refresh Token or Network Error.";
                            }
                        });
                        this.refreshTokenInput.set("");
                        this.isPlusExpanded = false;
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.sameLine();
                    if (ImGui.button((String)"Cancel", 80.0f, 25.0f)) {
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.popStyleColor(3);
                    ImGui.popStyleVar();
                    ImGui.endPopup();
                }
                if (ImGui.beginPopupModal((String)"SessionPopup", 64)) {
                    String token;
                    String clipboardText;
                    ImGui.text((String)"Enter/Paste Session Token:");
                    ImGui.inputText((String)"##sesstoken", (ImString)this.sessionTokenInput);
                    ImGui.sameLine();
                    ImGui.pushStyleColor(21, 0.2f, 0.2f, 0.2f, 0.8f);
                    ImGui.pushStyleColor(22, 0.2f, 0.2f, 0.2f, 0.8f);
                    ImGui.pushStyleColor(23, 0.2f, 0.2f, 0.2f, 0.8f);
                    if (ImGui.button((String)"PASTE") && !(clipboardText = mc.keyboardHandler.getClipboard().trim()).isEmpty()) {
                        this.sessionTokenInput.set(clipboardText);
                    }
                    ImGui.popStyleColor(3);
                    ImGui.spacing();
                    ImGui.pushStyleVar(12, 4.0f);
                    ImGui.pushStyleColor(21, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(22, 0.15f, 0.15f, 0.15f, 0.8f);
                    ImGui.pushStyleColor(23, 0.15f, 0.15f, 0.15f, 0.8f);
                    if (ImGui.button((String)"Add Account", 120.0f, 25.0f) && !(token = this.sessionTokenInput.get().trim()).isEmpty() && token.startsWith("eyJ")) {
                        this.fetchAndAddSessionAccount(token, "Session");
                        this.sessionTokenInput.set("");
                        this.isPlusExpanded = false;
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.sameLine();
                    if (ImGui.button((String)"Cancel", 80.0f, 25.0f)) {
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.popStyleColor(3);
                    ImGui.popStyleVar();
                    ImGui.endPopup();
                }
                ImGui.endChild();
            }
            ImGui.endChild();
        }
        ImGui.end();
        ImGui.popStyleVar(3);
    }

    public static class AltAccount {
        public String name;
        public String uuid;
        public String token;
        public String type;

        public AltAccount(String name, String uuid, String token, String type) {
            this.name = name;
            this.uuid = uuid;
            this.token = token;
            this.type = type;
        }
    }
}

