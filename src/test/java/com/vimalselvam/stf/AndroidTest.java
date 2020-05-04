package com.vimalselvam.stf;

import com.vimalselvam.DeviceApi;
import com.vimalselvam.STFService;
import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class AndroidTest{
    private static final String STF_SERVICE_URL = "your server url";  // Change this URL
    private static final String ACCESS_TOKEN = "your access token";  // Change this access token

    private AndroidDriver androidDriver;
    private String deviceSerial;
    private AppiumDriverLocalService service;
    private DeviceApi deviceApi;
    private String remoteConnectUrl;

    private String systemPort;


    @Factory(dataProvider = "parallelDp")
    public AndroidTest(String deviceSerial, String systemPort) {
        this.deviceSerial = deviceSerial;
        this.systemPort = systemPort;
    }

    private void createAppiumService() {
        this.service = new AppiumServiceBuilder().usingAnyFreePort().build();
        this.service.start();
    }

    private void connectToStfDevice() throws MalformedURLException, URISyntaxException {
        STFService stfService = new STFService(STF_SERVICE_URL,
                ACCESS_TOKEN);
        this.deviceApi = new DeviceApi(stfService);
        this.remoteConnectUrl = this.deviceApi.connectDevice(this.deviceSerial);
         connectDeviceToAdb(this.remoteConnectUrl);
    }

    private void connectDeviceToAdb(String connectUrl) {
        String[] command = { "/bin/bash", "-l", "-c", "adb connect " + connectUrl };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();
            p.waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String _temp = null;
            List<String> line = new ArrayList<String>();
            while ((_temp = in.readLine()) != null) {
                line.add(_temp);
            }
            System.out.println("result after command: " + line);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @BeforeClass
    public void setup() throws MalformedURLException, URISyntaxException {
        System.out.println("SystemPort " + systemPort);
        connectToStfDevice();
        createAppiumService();
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "ANDROID");
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "ANDROID");
        desiredCapabilities.setCapability("udid",  this.remoteConnectUrl);
        desiredCapabilities.setCapability("systemPort", this.systemPort);
        desiredCapabilities.setCapability(MobileCapabilityType.NO_RESET, false);
        desiredCapabilities.setCapability(MobileCapabilityType.APP,
                new File("src/test/resources/ApiDemos-debug.apk").getAbsolutePath());


        androidDriver = new AndroidDriver(this.service.getUrl(), desiredCapabilities);
    }

    @Test
    public void currentActivityTest() throws InterruptedException {
        Assert.assertEquals(androidDriver.currentActivity(), ".ApiDemos", "Activity not match");
    }

    @Test(dependsOnMethods = {"currentActivityTest"})
    public void scrollingToSubElement() {
        androidDriver.findElementByAccessibilityId("Views").click();
        AndroidElement list = (AndroidElement) androidDriver.findElement(By.id("android:id/list"));
        MobileElement radioGroup = list
                .findElement(MobileBy
                        .AndroidUIAutomator("new UiScrollable(new UiSelector()).scrollIntoView("
                                + "new UiSelector().text(\"Radio Group\"));"));
        Assert.assertNotNull(radioGroup.getLocation());
    }

    @AfterClass
    public void tearDown() {
        if (androidDriver != null) {
            androidDriver.quit();
        }

        if (this.service.isRunning()) {
            service.stop();
             this.deviceApi.releaseDevice(this.deviceSerial);
        }
    }

    @DataProvider
    static Object[][] parallelDp() {
        return new Object[][] {
            {"4200caca9bbe5400", "8200"}/*,    // Change the device serial
            {"ZW2222SMLP","8204"},*/    // Change the device serial
        };
    }
}
