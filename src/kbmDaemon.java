import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import kusoBotMaker.BotAccount;
import kusoBotMaker.DataContainer;
import kusoBotMaker.KbmConnectServer;
import kusoBotMaker.KbmUtil;
import kusoBotMaker.enumBotAcountStatus;

public class kbmDaemon implements Daemon {
	class BotAccountDaemon extends BotAccount {
		public BotAccountDaemon(long User_ID) {
			super(User_ID);
			// TODO 自動生成されたコンストラクター・スタブ
			putBotAccount(this);
		}

		public BotAccountDaemon(long User_ID, String consumerKey, String consumerSecret, String Access_Token,
				String Access_Token_Secret, boolean Enable, long normalPostInterval, long pauseTime, boolean replyRt) {
			super(User_ID, consumerKey, consumerSecret, Access_Token, Access_Token_Secret, Enable, normalPostInterval,
					pauseTime, replyRt);
			// TODO 自動生成されたコンストラクター・スタブ
			putBotAccount(this);
		}

		@Override
		public void pauseBot() {
			// TODO 自動生成されたメソッド・スタブ
			super.pauseBot();
			sentStatus(this);
		}

		@Override
		public Boolean startBot() {
			// TODO 自動生成されたメソッド・スタブ
			boolean rt = super.startBot();
			sentStatus(this);
			// this.startBotTwitterStream();
			return rt;
		}

		@Override
		public Boolean stopBot() {
			// TODO 自動生成されたメソッド・スタブ
			boolean rt = super.stopBot();
			sentStatus(this);
			// this.stoptBotTwitterStream();
			return rt;
		}

	}

	class BotActiveCheckThread extends Thread {
		// List<BotAccount> botAccounts;

		public BotActiveCheckThread() {
			// TODO 自動生成されたコンストラクター・スタブ
			// this.botAccounts = botAccounts;
		}

		/*
		 * (非 Javadoc)
		 *
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			// TODO 自動生成されたメソッド・スタブ
			super.run();
			ResultSet rs = BotAccount.GetAccessToken();
			try {
				while (rs.next()) {
					if (!update(rs)) {
						BotAccountDaemon botAccount = new BotAccountDaemon(rs.getLong("User_ID"),
								rs.getString("Consumer_Key"), rs.getString("Consumer_Secret"),
								rs.getString("Access_Token"), rs.getString("Access_Token_Secret"),
								rs.getBoolean("bot_enable"), rs.getLong("normal_post_interval"),
								rs.getLong("pause_time"), rs.getBoolean("replytoRT"));
						if (botAccount.Enable) {
							botAccount.startBot();
						} else {
							botAccount.stopBot();
						}
					}

				}
				rs.close();
			} catch (SQLException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}

		}

		private boolean update(ResultSet rs) throws SQLException {
			boolean rt = false;
			long getUserID = rs.getLong("User_ID");
			boolean b = rs.getBoolean("bot_enable");

			for (BotAccountDaemon botAccount : mapbotAccountDaemon.values()) {
				if (botAccount.User_ID == getUserID) {
					if (botAccount.Enable != b) {
						botAccount.Enable = b;
						if (b == true) {
							botAccount.startBot();
						} else {
							botAccount.stopBot();
						}
					}
					rt = true;
				}
			}
			return rt;
		}
	}

	class KbmConnectServerDaemon extends KbmConnectServer {
		@Override
		public void execContainer(DataContainer container) {
			super.execContainer(container);
			container.setBotAcountStatus(ChangeBotStatus(container));
		}
	}

	class UtilResetThread extends Thread {
		@Override
		public void run() {
			KbmUtil.init();
		}
	}

	private KbmConnectServerDaemon kbmConnectServerDaemon;

	private Map<Long, BotAccountDaemon> mapbotAccountDaemon;

	public kbmDaemon() {
		// TODO 自動生成されたコンストラクター・スタブ

	}

	private enumBotAcountStatus ChangeBotStatus(kusoBotMaker.DataContainer dtCon) {
		BotAccountDaemon botAccount = getBotAccount(dtCon.getBotAccountID());
		switch (dtCon.getSocketMode()) {
		case BOT_ADD:
			if (botAccount != null) {
				botAccount.stopBot();
				deletegBotAccount(botAccount);
				botAccount = null;
			}
			botAccount = new BotAccountDaemon(dtCon.getBotAccountID());
			botAccount.startBot();
			KbmUtil.addbotAccounts(botAccount);
			break;
		case BOT_DEL:
			if (botAccount != null) {
				deletegBotAccount(botAccount);
				botAccount.deleteBot();
				botAccount = null;
			}
			break;
		case BOT_START:
			if (botAccount != null) {
				botAccount.startBot();
			}
			break;
		case BOT_STOP:
			if (botAccount != null) {
				botAccount.stopBot();
			}
			break;
		case BOT_STATUS:
			if (botAccount != null) {
				sentStatus(botAccount);
			}
			break;
		case CLOSE_SOCKET:
			break;
		default:
			break;
		}
		return (botAccount != null) ? botAccount.getEnumBotAcountStatus() : enumBotAcountStatus.NOTRESV;
	}
	private void deletegBotAccount(BotAccountDaemon BotAccount) {
		mapbotAccountDaemon.remove(BotAccount.User_ID);

	}

	@Override
	public void destroy() {
		// TODO 自動生成されたメソッド・スタブ
		// botAccounts = null;
	}

	public BotAccountDaemon getBotAccount(long User_ID) {
		return mapbotAccountDaemon.get(User_ID);
	}

	@Override
	public void init(DaemonContext arg0) throws DaemonInitException, Exception {
		// TODO 自動生成されたメソッド・スタブ

	}

	// List<BotAccount> botAccounts;
	public static void main(String[] args) {
		String logPath = System.getProperty("java.class.path");
		String dirPath = logPath.substring(0, logPath.lastIndexOf(File.separator) + 1);
		String file = dirPath + "kbm.log";
		PrintStream out;
		try {
			out = new PrintStream(file);
			System.setOut(out);
		} catch (FileNotFoundException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
		System.out.println(dirPath);
		kbmDaemon kbmDaemon = new kbmDaemon();

		try {
			kbmDaemon.start();

		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	private void putBotAccount(BotAccountDaemon botAccountDaemon) {
		BotAccountDaemon oldbotAccountDaemon = mapbotAccountDaemon.put(botAccountDaemon.User_ID, botAccountDaemon);
		if (oldbotAccountDaemon != null) {
			oldbotAccountDaemon.stopBot();
			oldbotAccountDaemon = null;
		}

	}

	private void sentStatus(BotAccountDaemon botAccount) {
		// executorServiceSocketConnect.execute(getSocketServer().SendStatusFactory(User_ID,
		
		kbmConnectServerDaemon
				.sendContainer(new DataContainer(botAccount.User_ID, botAccount.getEnumBotAcountStatus()));
	}

	@Override
	public void start() throws Exception {
		// TODO 自動生成されたメソッド・スタブ

		kbmConnectServerDaemon = new KbmConnectServerDaemon();
		kbmConnectServerDaemon.setDaemon(true);
		kbmConnectServerDaemon.setName("KBM通信用サーバ");
		kbmConnectServerDaemon.start();

		mapbotAccountDaemon = new HashMap<Long, kbmDaemon.BotAccountDaemon>();

		ResultSet rs = BotAccount.GetAccessToken();
		try {
			while (rs.next()) {
				BotAccountDaemon botAccount = new BotAccountDaemon(rs.getLong("User_ID"), rs.getString("Consumer_Key"),
						rs.getString("Consumer_Secret"), rs.getString("Access_Token"),
						rs.getString("Access_Token_Secret"), rs.getBoolean("bot_enable"),
						rs.getLong("normal_post_interval"), rs.getLong("pause_time"), rs.getBoolean("replytoRT"));
				if (botAccount != null && botAccount.Enable) {
					botAccount.startBot();
					// Thread.sleep(500);
				}
			}
			rs.close();
		} catch (SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
		// executorService.scheduleAtFixedRate(new BotActiveCheckThread(), 0, 1,
		// TimeUnit.MINUTES);
		executorService.scheduleAtFixedRate(new UtilResetThread(), 0, 1, TimeUnit.HOURS);
	}

	@Override
	public void stop() throws Exception {

	}
}
