package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.api.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.accounts.Account;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;


@RunWith(SoundCloudTestRunner.class)
public class SoundCloudApplicationTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private AccountOperations accountOperations;

    @Test
    public void shouldOnlyHaveOneLauncherActivity() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new File("../app/AndroidManifest.xml"));
        NodeList nl = (NodeList) XPathFactory.newInstance().newXPath().compile("//activity/intent-filter/category").evaluate(doc, XPathConstants.NODESET);
        int launchers = 0;
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            NamedNodeMap attributes = n.getAttributes();
            if (attributes != null) {
                Node name = attributes.getNamedItem("android:name");
                if (name != null && "android.intent.category.LAUNCHER".equals(name.getNodeValue())) {
                    launchers++;
                }
            }
        }
        expect(launchers).toEqual(1);
    }

    @Test
    public void shouldHaveOpenGLEnabled() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new File("../app/AndroidManifest.xml"));
        NodeList nl = (NodeList) XPathFactory.newInstance().newXPath().compile("//application").evaluate(doc, XPathConstants.NODESET);
        expect(nl.getLength()).toEqual(1);
        Node app = nl.item(0);
        expect(app.getAttributes().getNamedItem("android:hardwareAccelerated").getNodeValue()).toEqual("true");
    }

    @Test
    public void shouldPublishUserChangedEventWhenAddingNewAccount() {
        final User user = new User();
        final Token token = new Token("123", "456");
        final SignupVia signupVia = SignupVia.API;

        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        SoundCloudApplication application = new SoundCloudApplication(eventBus, accountOperations);
        Account account = new Account("soundcloud", "com.soundcloud.account");
        when(accountOperations.addOrReplaceSoundCloudAccount(user, token, signupVia)).thenReturn(account);

        application.addUserAccountAndEnableSync(user, token, signupVia);

        CurrentUserChangedEvent event = eventMonitor.verifyEventOn(EventQueue.CURRENT_USER_CHANGED);
        expect(event.getKind()).toBe(CurrentUserChangedEvent.USER_UPDATED);
    }
}
