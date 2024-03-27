package de.minekonst.mariokartwiiai.server;

import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;
import javax.imageio.ImageIO;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.ai.AI;
import de.minekonst.mariokartwiiai.server.gui.ServerMapViewer;

public class Telegram {

    private static final long[] USERS = {};

    private static TelegramBot bot;
    private static AIServer server;
    private static volatile boolean hasConnection = true;

    private Telegram() {
    }

    public static void broadcast(String message, Object... format) {
        broadcast(String.format(message, format));
    }

    public static void broadcast(String message) {
        if (!hasConnection) {
            return;
        }

        for (long id : USERS) {
            SendMessage request = new SendMessage(id, message);
            try {
                bot.execute(request);
            }
            catch (Exception ex) {
                Main.log("Failed to broadcast: %s", ex.toString());
            }
        }
    }

    static void init(AIServer s) {
        server = s;

        bot = new TelegramBot("");
        bot.setUpdatesListener(Telegram::onUpdate);
        Main.log("Telegram Bot online");

        Thread t = new Thread(() -> {
            while (true) {
                boolean old = hasConnection;
                try {
                    hasConnection = InetAddress.getByName("1.1.1.1").isReachable(1_000);
                }
                catch (Exception ex) {
                    hasConnection = false;
                }

                if (old != hasConnection) {
                    if (hasConnection) {
                        Main.log("Internet connection is back up again");
                        bot.setUpdatesListener(Telegram::onUpdate);
                    }
                    else {
                        Main.log("Lost internet conncetion. Telegram bot will not work");
                        bot.removeGetUpdatesListener();
                    }
                }

                TimeUtils.sleep(15_000);
            }
        }, "Internet connection checker");
        t.setDaemon(true);
        t.start();
    }

    private static int onUpdate(List<Update> updates) {
        for (Update u : updates) {
            if (u.message() == null) {
                continue;
            }
            String text = u.message().text();
            String username = u.message().chat().username();
            long id = u.message().chat().id();

            if (id != 358897135) {
                sendReply("Operation not permitted", u.message());
                continue;
            }

            if (text != null) {
                Main.log("%s (%d) wrote: \"%s\"", username, id, text);
                reply(u.message());
            }
            else {
                Main.log("Recivied a message from %s (%d) which is not a text message", username, id);
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private static void reply(Message m) {
        if (m.text().equals("/status")) {
            AI<?, ?> ai = server.getAI();
            if (ai != null) {
                sendReply(String.format("=== Loaded AI: \"%s\"\n"
                        + "Era %d, Species %d, Generation: %d\n"
                        + "Score: %.2f Learntime: %s\n"
                        + "=== Scheduler is %s\n%s",
                        ai.getProperties().getXmlFile().getName(), ai.getProperties().getEra().getValue(),
                        ai.getProperties().getSpecies().getValue(), ai.getProperties().getGeneration().getValue(),
                        ai.getProperties().getScore().getValue(), ai.getLearnTime(),
                        server.isAIActive() ? "active" : "not active", ai.getState()), m);
            }
            else {
                sendReply("No AI loaded at the time", m);
            }
        }
        else if (m.text().equals("/clients")) {
            List<RemoteDriver> list = server.getRemoteDrivers();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Currently are %d Clients conncted\n", list.size()));
            for (RemoteDriver r : list) {
                sb.append(String.format("#%d | %s | %s | %d ms\n",
                        r.getServerClient().getID(), r.getServerClient().getIP(),
                        r.getState(), r.getPing()));
            }
            sendReply(sb.toString(), m);
        }
        else if (m.text().equals("/map")) {
            if (server.getAI() == null) {
                sendReply("Cannot print map: No AI active", m);
            }
            else {
                ServerMapViewer c = server.getGui().getMapViewer();
                BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D gi = image.createGraphics();
                c.drawRaw(gi);
                gi.dispose();
                ByteArrayOutputStream arr = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "png", arr);
                    SendPhoto p = new SendPhoto(m.chat().id(), arr.toByteArray());
                    p.replyToMessageId(m.messageId());
                    bot.execute(p);
                }
                catch (Exception ex) {
                    sendReply("Error: " + ex.toString(), m);
                }

            }
        }
        else {
            if (server.getAI() == null) {
                sendReply("Unknown command (Could not check AI, because no AI is loaded)", m);
            }
            else {
                String r = server.getAI().onCommand(m.text());
                if (r != null) {
                    sendReply(r, m);
                }
                else {
                    sendReply("Unknown command", m);
                }
            }
        }
    }

    private static void sendReply(String msg, Message to) {
        SendMessage request = new SendMessage(to.chat().id(), msg);
        request.replyToMessageId(to.messageId());
        bot.execute(request);
    }

}
