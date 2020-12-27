package medcluerunner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.dreambot.api.Client;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankMode;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.emotes.Emote;
import org.dreambot.api.methods.emotes.Emotes;
import org.dreambot.api.methods.fairyring.FairyLocation;
import org.dreambot.api.methods.fairyring.FairyRings;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.input.Keyboard;
import org.dreambot.api.methods.input.mouse.MouseSettings;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Map;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.settings.PlayerSettings;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.walking.pathfinding.impl.web.WebFinder;
import org.dreambot.api.methods.walking.web.node.AbstractWebNode;
import org.dreambot.api.methods.walking.web.node.impl.BasicWebNode;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;

@ScriptManifest(author = "Lyuda & Domste", description = "Runs medium clue's", name = "Med clue runner", category = Category.MAGIC, version = 1)
public class Main extends AbstractScript{

	State state;
	
	//DATABASE UPDATES
	private String dbbotname = "null";
	private int dbbotworld = 0;
	private String dbbottask = "null";
	private String dbbotruntime = "null";
	private int dbbotrangerboots = 0;
	private int dbbotcluestotal = 0;
	private int dbbotcluesperhour = 0;
	private int dbbotonline = 0;

	//TIMERS
	private long timeBegan;
	private long timeRan;
	
	/** GRAPHICS START **/
	private final Image bg = getImage("https://i.imgur.com/9FDM8Zj.jpg");
	private final Image mole = getImage("https://i.imgur.com/h0R2seR.png");
	Point[] lastPositions = new Point[15];
	private long angle;
	private AffineTransform oldTransform;
	//BUTTON TO CLOSE PAINT
	Point p;
	boolean hide = false;
	Rectangle close = new Rectangle(442, 357, 57, 20);
	Rectangle open = new Rectangle(442, 357, 57, 20);

	public void onMouse(MouseEvent e){
		p = e.getPoint();
		if (close.contains(p) && !hide) {
			hide = true;
		} else if (open.contains(p) && hide) {
			hide = false;
		}
	}
	//BUTTON TO CLOSE PAINT

	public void onPaint(Graphics2D g2d) {
		Point currentPosition = Mouse.getPosition();

		// Shift all elements down and insert the new element
		for(int i=0;i<lastPositions.length - 1;i++){
			lastPositions[i]=lastPositions[i+1];
		}
		lastPositions[lastPositions.length - 1] = new Point(currentPosition.x, currentPosition.y);

		// This is the point before the new point to draw to
		Point lastpoint = null;

		Color mColor = new Color(59, 237, 196);
		//Go in reverse
		for(int i=lastPositions.length - 1;i>=0;i--)
		{
			Point p = lastPositions[i];
			if(p != null)
			{
		    	if(lastpoint == null)
		    		lastpoint = p;

		    	g2d.setColor(mColor);
		    	g2d.drawLine(lastpoint.x, lastpoint.y, p.x, p.y);
			}
			lastpoint = p;

			//Every 2 steps - mouse fade out
			if(i % 2 == 0)
				mColor = mColor.darker();
		}

		oldTransform = g2d.getTransform();

		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(1));
		g2d.drawLine(currentPosition.x + 4, currentPosition.y - 4, currentPosition.x - 4, currentPosition.y + 4);
		g2d.drawLine(currentPosition.x - 4, currentPosition.y - 4, currentPosition.x + 4, currentPosition.y + 4);

		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(1));
		g2d.fillOval(currentPosition.x-2, currentPosition.y-2, 4, 4);

		if (getLocalPlayer().isAnimating() || getLocalPlayer().isMoving()) {
			g2d.setStroke(new BasicStroke(2));
			g2d.setColor(Color.WHITE);
			g2d.rotate(Math.toRadians(angle+=6), currentPosition.x, currentPosition.y);
			g2d.draw(new Arc2D.Double(currentPosition.x-12, currentPosition.y-12, 24, 24, 330, 60, Arc2D.OPEN));
			g2d.draw(new Arc2D.Double(currentPosition.x-12, currentPosition.y-12, 24, 24, 151, 60, Arc2D.OPEN));
			g2d.setTransform(oldTransform);
		}
	}

	@Override
	public void onPaint(Graphics g) {
		
		timeRan = System.currentTimeMillis() - this.timeBegan;
		dbbotcluesperhour = (int)(dbbotcluestotal / ((System.currentTimeMillis() - timeBegan) / 3600000.0D));
		if (!hide){
			g.drawImage(bg, 7, 345, 505, 129, null);
			//PAINT INFO HERE
			g.setColor(Color.WHITE);
		    g.drawString("Run time: " + ft(timeRan), 25, 363);
		    g.drawString("World: "+ dbbotworld, 25, 383);
		    g.drawString("Task: "+ dbbottask, 25, 403);
		    g.drawString("Total clues: " + dbbotcluestotal, 25, 423);
		    g.drawString("Clues per hour: " + dbbotcluesperhour, 25, 443);
		    g.drawString("Ranger boots: " + dbbotrangerboots, 25, 463);
			g.drawString("Authors: Lyuda & Xiao Xiao", 356, 463);
			
			g.setColor(Color.RED);
			g.drawRect(442, 357, 57, 20);
			g.drawString("HIDE", 457, 372);
		}

		if (hide){
			g.setColor(Color.GREEN);
			g.drawRect(442, 357, 57, 20);
			g.drawString("SHOW", 453, 372);
		}
		
		g.drawImage(mole, 461, 310, 60, 40, null);
	}
	
	private String ft(long duration) {
		String res = "";
		long days = TimeUnit.MILLISECONDS.toDays(duration);
		long hours = TimeUnit.MILLISECONDS.toHours(duration)
		- TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
		- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS
		.toHours(duration));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
		- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
		.toMinutes(duration));
		if (days == 0) {
			res = (hours + ":" + minutes + ":" + seconds);
		} else {
			res = (days + ":" + hours + ":" + minutes + ":" + seconds);
		}
		return res;
		}

		private Image getImage(String url) {
		 try {
			 return ImageIO.read(new URL(url));
		 }
		 catch (IOException e) {}
		 return null;
 	}
	/** GRAPHICS END **/
	
		
	/** PROGRESS RECORDING VARIABLES **/
	private long timeBeganClue;
	private int previousClue = 0;
	private int currentClue = 0;
	private String currentCluestr;
	/** END OF PROGRESS RECORDING VARIABLES **/

	
	/** CASE SPECIFIC VARIABLES **/
	private int failsafeopeningimplings = 0;
	private int setupcomplete = 0;
	private int backing = 0;
	int[] membersworlds = {302, 303, 304, 305, 306, 307, 309, 310, 311, 312, 313, 314, 315, 317, 319, 320, 321, 322, 323, 325, 327, 328, 329, 331, 332, 333, 334, 336, 337, 338, 339, 340, 341, 342, 344, 346, 347, 348, 350, 351, 352, 354, 355, 356, 357, 358, 359, 360, 362, 365, 367, 368, 369, 370, 374, 375, 376, 377, 378, 386, 387, 388, 389, 390, 395, 421, 422, 424, 443, 444, 445, 446, 463, 464, 465, 466, 477, 478, 479, 480, 481, 482, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 531, 532, 533, 534, 535};
	/** END OF CASE SPECIFIC VARIABLES **/
	
	
	/** GE PRICE VARIABLES **/
	int[] lowsupplies = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	String[] sellable = {"Holy sandals", "Spiked manacles", "Wizard boots", "Climbing boots (g)", "Skills necklace", "Amulet of glory", "Ring of wealth", "Combat bracelet", "Impling jar", "Purple sweets", "Red firelighter", "Green firelighter", "Gold bar", "Blue firelighter", "Purple firelighter", "White firelighter", "Gnomish firelighter", "Charge dragonstone jewellery scroll", "Nardah teleport", "Mos le'harmless teleport", "Mort'ton teleport", "Feldip hills teleport", "Lunar isle teleport", "Piscatoris teleprot", "Pest control teleport", "Tai bwo wannai teleport", "Lumberyard teleport", "Iorwerth camp teleport", "Holy blessing", "Unholy blessing", "Peaceful blessing", "War blessing", "Honourable blessing", "Ancient blessing", "Master scroll book (empty)", "Saradomin page 1", "Saradomin page 2", "Saradomin page 3", "Saradomin page 4", "Zamorak page 1", "Zamorak page 2", "Zamorak page 3", "Zamorak page 4", "Guthix page 1", "Guthix page 2", "Guthix page 3", "Guthix page 4", "Bandos page 1", "Bandos page 2", "Bandos page 3", "Bandos page 4", "Armadyl page 1", "Armadyl page 2", "Armadyl page 3", "Armadyl page 4", "Ancient page 1", "Ancient page 2", "Ancient page 3", "Ancient page 4", "Adamant full helm", "Adamant platebody", "Adamant platelegs", "Adamant longsword", "Adamant dagger", "Adamant battleaxe", "Adamant axe", "Adamant pickaxe", "Yew shortbow", "Fire battlestaff", "Yew longbow", "Amulet of power", "Yew comp bow", "Strength amulet (t)", "Air rune", "Mind rune", "Water rune", "Earth rune", "Fire rune", "Chaos rune", "Nature rune", "Law rune", "Death rune", "Lobster", "Swordfish", "Adamant full helm (t)", "Adamant platebody (t)", "Adamant platelegs (t)", "Adamant plateskirt (t)", "Adamant full helm (g)", "Adamant platebody (g)", "Adamant platelegs (g)", "Adamant plateskirt (g)", "Adamant kiteshield (g)", "Adamant shield (h1)", "Adamant shield (h2)", "Adamant shield (h3)", "Adamant shield (h4)", "Adamant shield (h5)", "Adamant helm (h1)", "Adamant helm (h2)", "Adamant helm (h3)", "Adamant helm (h4)", "Adamant helm (h5)", "Adamant platebody (h1)", "Adamant platebody (h2)", "Adamant platebody (h3)", "Adamant platebody (h4)", "Adamant platebody (h5)", "Mithril full helm (g)", "Mithril platebody (g)", "Mithril platelegs (g)", "Mithril plateskirt (g)", "Mithril kiteshield (g)", "Mithril full helm (t)", "Mithril platebody (t)", "Mithril platelegs (t)", "Mithril plateskirt (t)", "Mithril kiteshield (t)", "Green d'hide body (g)", "Green d'hide body (t)", "Green d'hide chaps (g)", "Green d'hide chaps (t)", "Saradomin mitre", "Saradomin cloak", "Guthix mitre", "Guthix cloak", "Zamorak mitre", "Zamorak cloak", "Ancient mitre", "Ancient cloak", "Ancient stole", "Ancient crozier", "Armadyl mitre", "Armadyl cloak", "Armadyl stole", "Armadyl crozier", "Bandos mitre", "Bandos cloak", "Bandos stole", "Bandos crozier", "Red boater", "Green boater", "Orange boater", "Black boater", "Blue boater", "Pink boater", "Purple boater", "White boater", "Red headband", "Black headband", "Brown headband", "White headband", "Blue headband", "Gold headband", "Pink headband", "Green headband", "Crier hat", "Adamant cane", "Cat mask", "Penguin mask", "Leprechaun hat", "Crier coat", "Crier bell", "Arceuus banner", "Piscarilius banner", "Hosidius banner", "Shayzien banner", "Lovakengj banner", "Cabbage round shield", "Wolf mask", "Wolf cloak", "Black leprechaun hat", "Black unicorn mask", "White unicorn mask", "Purple elegant shirt", "Purple elegant blouse", "Purple elegant legs", "Purple elegant skirt", "Black elegant shirt", "White elegant blouse", "Black elegant legs", "White elegant skirt", "Pink elegant shirt", "Pink elegant blouse", "Pink elegant legs", "Pink elegant skirt", "Gold elegant shirt", "Gold elegant blouse", "Gold elegant legs", "Gold elegant skirt", "Mithril pickaxe", "Curry leaf", "Snape grass", "Oak plank", "Empty candle lantern", "Gold ore", "Bar", "Unicorn horn", "Adamant kiteshield", "Blue d'hide chaps", "Red spiky vambraces", "Rune dagger", "Battlestaff", "Adamantite ore", "Slayer's respite", "Wild pie", "Watermelon seed", "Diamond"};
	//NOTE: it does not sell: green d'hide chaps & body and ranger boots.
	double percentagemarkup = 1.3;
	//prices are declared in onstart (using rsbuddy price API or with official api and mannually in case rsbuddy and offical apis are down
	int eclecticimplingjarprice = 0;
	int stampotprice = 0;
	int digsitetpprice = 0;
	int gamesnecklaceprice = 0;
	int skillsnecklaceprice = 0;
	int amuletofgloryprice = 0;
	int tptohouseprice = 0;
	int varrockteleportprice = 0;
	int lumbygraveyardtpprice = 0;
	int faladortpprice = 0;
	int fenkenstraintpprice = 0;
	int westardytpprice = 0;
	int ardytpprice = 0;
	int camelottpprrice = 0;
	int draynormanortpprice = 0;
	int sharkprice = 0;
	int runearrowprice = 0;
	int necklaceofpassageprice = 0;
	int ringofwealthprice = 0;
	int combatbraceletprice = 0;
	int ringofduelingprice = 0;
	/** END OF GE PRICE VARIABLES **/
	
	
	/** AREAS BANKING **/
	Area grandexchangearea = new Area(3143, 3513, 3186, 3468);
	Area grandexchangeareamedium = new Area(3160, 3493, 3169, 3486);
	Area grandexchangeareasmall = new Area(3161, 3491, 3168, 3488);
	Area varrockcentre = new Area(3200, 3442, 3225, 3417);
	Area edgevillecentre = new Area(3077, 3505, 3100, 3481);
	/** END OF AREAS BANKING **/
	
	
	/** CLUE VARIABLES **/
	int C12067teleportbugfix = 0;
	int teleported = 0;
	/** END OF CLUE VARIABLES **/
	
	/** CLUE AREAS**/
	Area draynorvillagefishing = new Area(3081, 3263, 3118, 3208);
	Area draynorvillagetp = new Area(3095, 3260, 3113, 3242);
	Area C2827tile = new Area(3091, 3227, 3091, 3227);
	Area karamjaarea = new Area(2715, 3207, 2939, 3125);
	Area karamjateleport = new Area(2909, 3182, 2927, 3165);
	Area C3590tile = new Area(2743, 3150, 2743, 3150);
	Area catherbytocamelot = new Area(2752, 3482, 2841, 3405);
	Area camelotteleport = new Area(2751, 3481, 2766, 3468);
	Area C12067area = new Area(2821, 3445, 2825, 3441);
	Area brimhavencentral = new Area(2783, 3193, 2815, 3146);
	Area southeasthousebrimhaven = new Area(2807, 3166, 2814, 3160);
	Area barbarianvillage = new Area(3073, 3428, 3092, 3410);
	Area digsitetoexamcentre = new Area(3308, 3428, 3382, 3329);
	Area chestroomexamcentre = new Area(3348, 3336, 3356, 3332);
	Area digsiteteleport = new Area(3318, 3416, 3327, 3407);
	Area chesttile7296 = new Area(3353, 3332, 3353, 3332);
	Area castlewarslobby = new Area(2435, 3100, 2446, 3079);
	Area brimhavenporttopeninsula = new Area(2688, 3240, 2786, 3186);
	Area brimhavenpeninsula = new Area(2688, 3220, 2705, 3200);
	Area C2805area = new Area(2696, 3206, 2696, 3206);
	Area WEBWALK2805BRIMHAVEN = new Area(2717, 3207, 2717, 3207);
	Area WEBNODEBRIMHAVENCENTER = new Area(2795, 3177, 2795, 3177);
	Area barbarianvillagewebnode = new Area(3081, 3424, 3081, 3424);
	Area faladorparktp = new Area(2990, 3382, 3004, 3368);
	Area faladorparkbig = new Area(2980, 3392, 3028, 3366);
	Area faladorparkbridge = new Area(2990, 3385, 2991, 3382);
	Area portpascariliusbig = new Area(1776, 3767, 1859, 3667);
	Area dockmasterhouse = new Area(1818, 3742, 1825, 3737);
	Area draynorvillagetoportsarim = new Area(2996, 3287, 3121, 3184);
	Area veosnode = new Area(3051, 3248, 3051, 3248);
	Area veosbig = new Area(3044, 3252, 3057, 3241);
	Area woodcuttingguildtp = new Area(1655, 3512, 1670, 3498);
	Area webnoderellekamarket = new Area(2643, 3681, 2643, 3681);
	Area keldagrimtorelleka = new Area(2612, 3736, 2757, 3609);
	Area wizardtowerbig = new Area(3085, 3198, 3131, 3143);
	Area wizardtowerbigfloor1 = new Area(new Tile(3085, 3198, 1), new Tile(3131, 3143, 1));
	Area wizardtowerbigfloor2 = new Area(new Tile(3085, 3198, 2), new Tile(3131, 3143, 2));
	Area wizardtowerfairyring = new Area(3107, 3150, 3109, 3148);
	Area rellekamarket = new Area(2636, 3683, 2650, 3668);
	Area lighthousebig = new Area(2490, 3654, 2533, 3605);
	Area insidelighthouse = new Area(2432, 4613, 2465, 4575);
	Area treegnomevillageto2809 = new Area(2451, 3208, 2561, 3129);
	Area treegnomevillagenotthrugate = new Area(2514, 3176, 2547, 3161);
	Area treegnomevillagethrugate = new Area(2514, 3160, 2544, 3149);
	Area elkoyto2809digspot = new Area(2468, 3207, 2509, 3130);
	Area digspot2809 = new Area(2478, 3157, 2478, 3157);
	Area gespirittree = new Area(3182, 3512, 3187, 3507);
	Area gespirittreelarge = new Area(3178, 3515, 3191, 3503);
	Area treegnomevillagegate = new Area(2515, 3161, 2515, 3161);
	Area treegnomevillagegatelarge = new Area(2514, 3164, 2524, 3161);
	Area digspot12051 = new Area(3312, 3375, 3312, 3375);
	Area woodcuttingguildtoshayzienring = new Area(1512, 3625, 1688, 3461);
	Area shayzienringstashunit = new Area(1534, 3592, 1535, 3590);
	Area shayzienringstashunitbig = new Area(1534, 3598, 1540, 3586);
	Area shayzienringmiddle = new Area(1543,3596,1546,3593);
	Area shayzienringssurrounding = new Area(1533, 3605, 1555, 3584);
	Area webnodebugfixareawoodcuttingguild = new Area(1661, 3515, 1662, 3514);
	Area webnoderellekamainhall = new Area(2658, 3667, 2659, 3666);
	Area rellekamainhall = new Area(2655, 3681, 2662, 3665);
	Area treegnomestrongholdspirittree = new Area(2454, 3452, 2466, 3440);
	Area treegnomestrongholdto2853 = new Area(2372, 3501, 2476, 3429);
	Area gnomeballfield = new Area(new Tile(2383, 3489), new Tile(2383, 3487), new Tile(2385, 3486), new Tile(2385, 3484), new Tile(2389, 3480), new Tile(2393, 3480), new Tile(2396, 3483), new Tile(2397, 3483), new Tile(2400, 3480), new Tile(2404, 3480), new Tile(2408, 3484), new Tile(2408, 3492), new Tile(2404, 3496), new Tile(2400, 3496), new Tile(2397, 3493), new Tile(2396, 3493), new Tile(2393, 3496), new Tile(2389, 3496), new Tile(2385, 3492), new Tile(2383, 3491));
	Area refereenpcarea = new Area(2386, 3488, 2387, 3487);
	Area spirittreesmall = new Area(3183, 3511, 3186, 3508);
	Area outsidegnomeballfield = new Area(2378, 3489, 2379, 3488);
	Area woodcuttingguildtohosidius = new Area(1642, 3578, 1774, 3460);
	Area allotmentpatchhosidius = new Area(1728, 3560, 1740, 3549);
	Area allotmentpatchhosidiusmiddle = new Area(1733, 3556, 1736, 3553);
	Area toweroflifetoardougnemonastery = new Area(2582, 3246, 2671, 3197);
	Area ardougnemonasterybig = new Area(2590, 3219, 2622, 3202);
	Area ardougnemonasterysmall = new Area(2605, 3210, 2606, 3209);
	Area varrockcentertochurch = new Area(3197, 3496, 3267, 3408);
	Area varrockchurchtoprightroom = new Area(3256, 3487, 3259, 3485);
	Area closechestvarrockchurh = new Area(3256,3487,3256,3487);
	Area camelottoflaxkeeper = new Area(2719, 3487, 2784, 3417);
	Area flaxkeeperareabig = new Area(new Tile(2736, 3450), new Tile(2736, 3446),new Tile(2737, 3445),new Tile(2737, 3439),new Tile(2739, 3437),new Tile(2741, 3437),new Tile(2742, 3436),new Tile(2746, 3436),new Tile(2747, 3437),new Tile(2751, 3437),new Tile(2752, 3437),new Tile(2752, 3443),new Tile(2752, 3452),new Tile(2746, 3452),new Tile(2745, 3453),new Tile(2744, 3453),new Tile(2743, 3454),new Tile(2741, 3454),new Tile(2739, 3452),new Tile(2738, 3452));
	Area flaxkeeperareasmall = new Area(2743, 3444, 2744, 3443);
	Area mcgruborswood = new Area(new Tile(2625, 3508),new Tile(2624, 3507),new Tile(2624, 3471),new Tile(2625, 3470 ),new Tile(2629, 3470),new Tile(2631, 3468),new Tile(2641, 3468),new Tile(2642, 3469),new Tile(2646, 3469 ),new Tile(2647, 3470),new Tile(2656, 3470),new Tile(2657, 3469),new Tile(2661, 3469),new Tile(2663, 3467 ),new Tile(2669, 3467),new Tile(2671, 3469),new Tile(2671, 3478),new Tile(2677, 3484),new Tile(2677, 3488 ),new Tile(2680, 3491),new Tile(2680, 3493),new Tile(2679, 3494),new Tile(2679, 3497),new Tile(2677, 3499 ),new Tile(2677, 3504),new Tile(2673, 3508),new Tile(2667, 3508),new Tile(2665, 3506),new Tile(2665, 3505 ),new Tile(2662, 3502),new Tile(2662, 3492),new Tile(2659, 3489),new Tile(2657, 3489),new Tile(2654, 3492 ),new Tile(2654, 3502),new Tile(2653, 3503),new Tile(2653, 3504),new Tile(2649, 3508),new Tile(2637, 3508 ),new Tile(2636, 3509),new Tile(2629, 3509),new Tile(2628, 3508));
	Area mcgruborswoodcrateareabig = new Area(2654, 3490, 2661, 3483);
	Area mcgruborswoodcrateareasmall = new Area(2657, 3488, 2658, 3487);
	Area canifistavern = new Area(new Tile(3491, 3480),new Tile(3505, 3480),new Tile(3505, 3471),new Tile(3503, 3471),new Tile(3500, 3468),new Tile(3488, 3468),new Tile(3488, 3478),new Tile(3490, 3478),new Tile(3491, 3479));
	Area CKStocanifis = new Area(3433, 3514, 3534, 3451);
	Area canifistavernsmall = new Area(3493, 3474, 3494, 3473);
	Area CKSteleport = new Area(3441, 3478, 3456, 3464);
	Area ardougnetptojericoshouse = new Area(2586, 3345, 2678, 3281);
	Area ardougnemarket = new Area(2651, 3318, 2672, 3294);
	Area jericoshouse = new Area(2611, 3326, 2617, 3323);
	Area jericoshousesmall = new Area(2615, 3324, 2616, 3323);
	Area drawerspotupstairsjericoshouse = new Area(new Tile(2611,3324, 1),new Tile(2611,3324, 1));
	Area digspot12043 = new Area(3120, 3383, 3120, 3383);
	Area draynormanortodigspot = new Area(3080, 3388, 3156, 3299);
	Area draynormanortp = new Area(3102, 3354, 3114, 3345);
	Area ardougnetoboat = new Area(2647, 3322, 2700, 3256);
	Area ardougneport = new Area(2669, 3279, 2686, 3265);
	Area ardougneportsmall = new Area(2674, 3275, 2675, 3274);
	Area rimmingtonporttobushpatch = new Area(2898, 3240, 2951, 3203);
	Area rimmingtonbushpatch = new Area(2935, 3226, 2945, 3217);
	Area rimmingtonbushpatchsmall = new Area(2941, 3221, 2942, 3220);
	Area slayertowerdigspot = new Area(3428, 3523, 3428, 3523);
	Area CKStoslayertower = new Area(3399, 3554, 3479, 3452);
	Area battlefieldofkhazadto3601 = new Area(2537, 3271, 2582, 3213);
	Area battlefieldofkhazadteleport = new Area(2549, 3264, 2560, 3252);
	Area crate3601big = new Area(2559, 3252, 2567, 3245);
	Area crate3601small = new Area(2564, 3247, 2565, 3246);
	Area crateobject3601 = new Area(2565, 3248, 2565, 3248);
	Area crate3609area = new Area(3498, 3507, 3498, 3507);
	Area canifisclothesshop = new Area(new Tile(3497, 3508), new Tile(3496, 3507), new Tile(3496, 3503), new Tile(3505, 3503), new Tile(3505, 3507),new Tile(3504, 3508));
	Area canifisclothesshopsmall = new Area(3497, 3506, 3498, 3505);
	Area digspot12045 = new Area(2382, 3467, 2382, 3467);
	Area treegnomestrongholdto12045 = new Area(2358, 3488, 2509, 3375);
	Area castlewarstoobservatory = new Area(2420, 3183, 2487, 3063);
	Area observatory = new Area(new Tile(2438, 3169),new Tile(2444, 3169),new Tile(2448, 3165),new Tile(2448, 3161),new Tile(2447, 3158),new Tile(2445, 3156),new Tile(2443, 3155),new Tile(2440, 3153),new Tile(2438, 3154),new Tile(2433, 3159),new Tile(2433, 3165 ));
	Area observatoryropeshortcut = new Area(2449, 3150, 2450, 3149);
	Area middleofobservatory = new Area(2439, 3161, 2439, 3161);
	Area slayertower2digspot = new Area(3442, 3515, 3442, 3515);
	Area drawers7298 = new Area(new Tile(2448, 4601, 1), new Tile(2448, 4601, 1));
	Area burthorpetosabacave = new Area(2847, 3592, 2912, 3527);
	Area sabacave = new Area(2259, 4768, 2280, 4743);
	Area burthorpeteleport = new Area(2890, 3557, 2909, 3538);
	Area sabacaveentrance = new Area(2857, 3577, 2858, 3576);
	Area wizardtowerstaircaseroom = new Area(new Tile(3103, 3166),new Tile(3105, 3166),new Tile(3106, 3165),new Tile(3106, 3164),new Tile(3109, 3161),new Tile(3107, 3159),new Tile(3103, 3159),new Tile(3103, 3162),new Tile(3102, 3163),new Tile(3102, 3165));
	Area wizardtowerinsidestaircaseroomsmall = new Area(3104, 3161, 3105, 3160);
	Area wizardtowerinside = new Area(new Tile(3103, 3166),new Tile(3105, 3166),new Tile(3106, 3165),new Tile(3107, 3166),new Tile(3107, 3167),new Tile(3112, 3167),new Tile(3116, 3163),new Tile(3116, 3158),new Tile(3112, 3154),new Tile(3107, 3154),new Tile(3103, 3158),new Tile(3103, 3162),new Tile(3102, 3163),new Tile(3102, 3165));
	Area traibornroom = new Area(new Tile(3110, 3166, 1),new Tile(3112, 3166, 1),new Tile(3115, 3163, 1),new Tile(3115, 3160, 1),new Tile(3110, 3160, 1));
	Area traibornroomsmall = new Area(new Tile(3111, 3162, 1), new Tile(3112, 3161, 1));
	Area karamjatovolcano = new Area(2840, 3206, 2930, 3136);
	Area karamjaundergroundentrancesmall = new Area(2856, 3167, 2857, 3166);
	Area karamjaunderground = new Area(2819, 9667, 2874, 9542);
	Area crandor = new Area(2805, 3316, 2873, 3220);
	Area crandorentranceareasmall = new Area(2832, 9658, 2833, 9657);
	Area C23136digspot = new Area (2828, 3234, 2828, 3234);
	Area climbingropecrandor = new Area(2833, 9657, 2833, 9657);
	Area afterdoor = new Area(2836, 9600, 2836, 9600);
	Area pastkaramjadungeondoor = new Area(2821, 9600, 2872, 9663);
	Area crandorentranceareadoor = new Area(2836, 9599, 2837, 9598);
	Area arceuustodarkessencemine = new Area(1611, 3926, 1807, 3807);
	Area arceuuslibraryteleport = new Area(1634, 3872, 1644, 3863);
	Area darkessenceminebig = new Area(1758, 3863, 1772, 3840);
	Area darkessenceminesmall = new Area(1763, 3854, 1764, 3853);
	Area memorialarea = new Area (3578, 3527, 3578, 3527);
    Area fenkenstrainscastletograveyard = new Area(3537, 3544, 3590, 3513);
    Area mausoleum = new Area(3486, 3585, 3518, 3561);
    Area morytaniaundergroundarea = new Area(3466, 9983, 3585, 9918);
    Area morytaniaundergroundarea2 = new Area(3523, 9974, 3583, 9920);
    Area fenkenstrainscastleteleport = new Area(3541, 3533, 3556, 3524);
    Area fenkenstrainsgraveyardsmall = new Area(3580, 3526, 3581, 3525);
    Area morytaniaundergroundladder = new Area(3501, 9970, 3502, 9969);
	Area morytaniaundergroundladdertile = new Area(3504, 9970, 3504, 9970);
	Area undergroundpastentrance = new Area (3510, 9957, 3510, 9957);
	Area undergroundafterentrance = new Area(new Tile(3494, 9967),new Tile(3507, 9979),new Tile(3519, 9966),new Tile(3513, 9959),new Tile(3511, 9958),new Tile(3511, 9954));
	Area undergroundbeforeentrance = new Area(3513, 9957, 3513, 9957);
	Area fairyrintotaibwowannai = new Area(2740, 3105, 2864, 2984);
	Area taibwowannaistash = new Area(2793, 3087, 2809, 3067);
	Area taibwowannaistashsmall = new Area(2801, 3080, 2802, 3079);
	Area taibwowannaifence = new Area(new Tile(2784, 3075),new Tile(2782, 3075),new Tile(2781, 3074),new Tile(2780, 3074),new Tile(2779, 3073),new Tile(2778, 3073),new Tile(2777, 3072),new Tile(2776, 3072),new Tile(2774, 3070),new Tile(2774, 3069),new Tile(2772, 3067),new Tile(2772, 3065),new Tile(2773, 3064),new Tile(2773, 3062),new Tile(2774, 3061),new Tile(2775, 3061),new Tile(2778, 3058),new Tile(2783, 3056),new Tile(2786, 3053),new Tile(2788, 3053),new Tile(2793, 3053),new Tile(2795, 3053),new Tile(2798, 3056),new Tile(2803, 3057),new Tile(2805, 3057),new Tile(2806, 3058),new Tile(2807, 3058),new Tile(2808, 3059),new Tile(2809, 3059),new Tile(2813, 3063),new Tile(2814, 3063),new Tile(2816, 3065),new Tile(2816, 3069),new Tile(2814, 3071),new Tile(2814, 3072),new Tile(2813, 3073),new Tile(2812, 3073),new Tile(2811, 3074),new Tile(2810, 3074),new Tile(2809, 3075),new Tile(2806, 3075),new Tile(2804, 3071),new Tile(2785, 3071));
	Area taibwowannaifencesmall = new Area(2802, 3070, 2803, 3069);
	Area draynorvillagetoham = new Area(3083, 3280, 3179, 3217);
	Area C2801digtile = new Area(3161, 3251, 3161, 3251);
	Area CKStomortmyrefungusgate = new Area(3425, 3480, 3462, 3450);
	Area mortmyrefungusgate = new Area(3435, 3466, 3453, 3455);
	Area mortmyrefungusgatesmall = new Area(3442, 3459, 3443, 3458);
	Area arceuustosoulaltar = new Area(1594, 3911, 1856, 3833);
	Area soulaltar = new Area(1805, 3864, 1827, 3841);
	Area soulaltarsmall = new Area(1815, 3857, 1816, 3856);
	Area rangingguildbox = new Area(2671, 3437, 2671, 3437);
	Area rangingguild = new Area(2645, 3452, 2691, 3408);
	Area rangingguildboxarea = new Area(new Tile(2667, 3446),new Tile(2670, 3446),new Tile(2676, 3440),new Tile(2669, 3434),new Tile(2662, 3441));
	Area rangingguildboxareasmall = new Area(2670, 3438, 2671, 3437);
	Area rangingguildgotthroughdoor = new Area(2659, 3437, 2659, 3437);
	Area cptkhaledhouse = new Area(1835, 3758, 1850, 3737);
	Area rimmingtonnexttochemistshouse = new Area(2924, 3209, 2924, 3209);
	Area rimmingtontochemistshouse = new Area(2894, 3242, 2951, 3188);
	Area varrocktovarrockcastle = new Area(3186, 3511, 3235, 3413);
	Area varrockcastleroaldsroom = new Area(new Tile(3220, 3479),new Tile(3225, 3479),new Tile(3225, 3475),new Tile(3226, 3474),new Tile(3226, 3471),new Tile(3225, 3470),new Tile(3225, 3469),new Tile(3221, 3469),new Tile(3219, 3471),new Tile(3219, 3474),new Tile(3220, 3475));
	Area varrockcastleroaldsroomsmall = new Area(3223, 3473, 3224, 3472);
	Area camelottocourthouse = new Area(2724, 3485, 2770, 3457);
	Area infrontofcourthousesmall = new Area(2730, 3476, 2731, 3475);
	Area infrontofcourthouse = new Area(2729, 3479, 2741, 3472);
	Area insidecourthousesmall = new Area(2733, 3468, 2734, 3467);
	Area insidecourthouse = new Area(2732, 3471, 2739, 3465);
	Area buildingeastofshayzienring = new Area(1556, 3605, 1567, 3587);
	Area buildingeastofshayzienringsmall = new Area(1562, 3598, 1563, 3597);
	Area ardougneteleporttozookeeper = new Area(2593, 3325, 2677, 3258);
	Area zookeeperarea = new Area(2597, 3293, 2608, 3281);
	Area zookeeperareasmall = new Area(2603, 3287, 2604, 3286);
	Area barbarianoutposttoagility = new Area(2502, 3594, 2568, 3528);
	Area barbarianoutpostteleport = new Area(2512, 3575, 2525, 3564);
	Area barbianoutpostagilityroom = new Area(new Tile(2550, 3560),new Tile(2554, 3560),new Tile(2554, 3542),new Tile(2529, 3542),new Tile(2529, 3551),new Tile(2528, 3552),new Tile(2528, 3557),new Tile(2545, 3557),new Tile(2546, 3558),new Tile(2546, 3556),new Tile(2550, 3556));
	Area stashunitbarbarianoutpost = new Area(2540, 3550, 2541, 3549);
	Area barbianoutpostbeforeagilityroompipe = new Area(2551, 3561, 2552, 3560);
	Area barbianoutpostbeforeagilityroom = new Area(2546, 3573, 2555, 3560);
	Area outposttotreegnomestrongholddoor = new Area(2421, 3397, 2486, 3327);
	Area treegnomestrongholddoor = new Area(2452, 3390, 2469, 3374);
	Area treegnomestrongholddoorsmall = new Area(2460, 3382, 2461, 3381);
	Area theoutpostteleport = new Area(2423, 3353, 2441, 3338);
	Area akstofeldiphills = new Area(2542, 2971, 2620, 2885);
	Area feldiphillsteleport = new Area(2564, 2963, 2578, 2952);
	Area C12033digspot = new Area(2593, 2899, 2593, 2899);
	Area clstopeninsula = new Area(2642, 3125, 2698, 3066);
	Area eastyanilleteleport = new Area(2679, 3086, 2685, 3079);
	Area C2803digspot = new Area(2680, 3111, 2680, 3111);
	Area arceuustolibrary = new Area(1589, 3893, 1674, 3777);
	Area arceuuslibrary = new Area(1623, 3817, 1642, 3798);
	Area arceuuslibrarysmall = new Area(1639, 3812, 1640, 3811);
	Area blptotzhaarswordshop = new Area(2418, 5174, 2500, 5104);
	Area blpteleport = new Area(2429, 5131, 2438, 5122);
	Area tzhaarswordshop = new Area(new Tile(2478, 5149),new Tile(2480, 5149),new Tile(2481, 5148),new Tile(2480, 5147),new Tile(2481, 5146),new Tile(2481, 5145),new Tile(2480, 5144),new Tile(2476, 5144),new Tile(2476, 5145),new Tile(2475, 5146),new Tile(2475, 5147));
	Area tzhaarswordshopsmall = new Area(2478, 5145, 2479, 5144);
	Area mortmyretoc7288digspot = new Area(3402, 3453, 3505, 3256);
	Area mortmyreteleport = new Area(3464, 3437, 3474, 3427);
	Area c7288digspot = new Area(3434, 3266, 3434, 3266);
	Area brimhavenporttohajedy = new Area(2748, 3248, 2796, 3185);
	Area brimhavenhajedysmall = new Area(2781, 3210, 2782, 3209);
	Area brimhavenhajedy = new Area(2775, 3218, 2787, 3207);
	Area cjrtosinclairmansion = new Area(2678, 3601, 2766, 3516);
	Area sinclairmansionupstairs = new Area(new Tile(2731, 3583, 1), new Tile(2748, 3573, 1));
	Area cinclairmansionstairssmall = new Area(2736, 3581, 2737, 3580);
	Area cinclairmansionstairs = new Area(2733, 3582, 2741, 3575);
	Area cjrteleport = new Area(2700, 3583, 2710, 3571);
	Area afterlargedoorsinclairmansion = new Area(2740, 3573, 2741, 3573);
	Area lumbridgegraveyardtocastle = new Area(3199, 3237, 3254, 3188);
	Area lumbridgecastlecooksroom = new Area(3205, 3217, 3212, 3212);
	Area lumbridgegraveyardtp = new Area(3236, 3204, 3253, 3188);
	Area lumbridgecastlecooksroomsmall = new Area(3207, 3216, 3208, 3215);
	Area rimmingtonboats = new Area(new Tile(2896, 3235, 1), new Tile(2928, 3214, 1));
	Area brimhavenboats = new Area(new Tile(2753, 3252, 1), new Tile(2786, 3221, 1));
	Area jericoshouseupstairs = new Area(new Tile(2608, 3329, 1), new Tile(2620, 3320, 1));
	Area southeasthousebrimhavenupstairs = new Area(new Tile(2801, 3171, 1), new Tile(2822, 3153, 1));
	Area insidelighthouseupstairs = new Area(new Tile(2435, 4610, 1), new Tile(2454, 4590, 1));	
	Area pohtoalithekebab = new Area(3328, 3009, 3380, 2939);
	Area alithekebab = new Area(3352, 2976, 3355, 2973);
	Area pohpolniveachtp = new Area(3336, 3006, 3345, 2998);	
	Area alithekebabsmall = new Area(3353, 2975, 3354, 2974);
	Area dlqtouzer = new Area(3403, 3105, 3522, 2998);
	Area dlqteleport = new Area(3420, 3021, 3426, 3014);
	Area C12035digspot = new Area(3510, 3075, 3510, 3075);
	Area C12035inbetweenspot = new Area(3451, 3076, 3451, 3076);
	Area clstonmz = new Area(2590, 3131, 2696, 3064);
	Area nmzsmall = new Area(2608, 3115, 2609, 3114);
	Area nmzbig = new Area(2604, 3120, 2614, 3109);
	Area varrockarchershop = new Area(new Tile(3231, 3428),new Tile(3235, 3428),new Tile(3235, 3425),new Tile(3237, 3423),new Tile(3237, 3420),new Tile(3230, 3420),new Tile(3230, 3427));
	Area varrockcentertoarchershop = new Area(3201, 3440, 3244, 3403);
	Area varrockarchershopsmall = new Area(3232, 3425, 3233, 3424);
	Area edgevilletobarbvillage = new Area(3049, 3520, 3134, 3384);
	Area digspot3592 = new Area(2387, 3435, 2387, 3435);
	Area duelarenatohospital = new Area(3309, 3289, 3398, 3212);
	Area duelarenateleport = new Area(3306, 3247, 3325, 3223);
	Area duelarenahospitalbig = new Area(3355, 3279, 3379, 3267);
	Area duelarenahospitalsmall = new Area(3362, 3276, 3363, 3275);
	Area portsarimtobiasbig = new Area(3019, 3228, 3035, 3208);
	Area portsarimtobiassmall = new Area(3027, 3217, 3028, 3216);
	Area drawerselementalworkshop = new Area(2709, 3478, 2709, 3478);
	Area rangingguildtoseersvillage = new Area(2630, 3514, 2748, 3408); 
	Area outsiderangingguild = new Area(2643, 3448, 2663, 3433);
	Area elementalworkshophouse = new Area(2709, 3482, 2716, 3476);
	Area elementalworkshophousesmall = new Area(2710, 3478, 2711, 3477);
	Area cirtomountkaruulm = new Area(1244, 3863, 1369, 3738);
	Area cirteleport = new Area(1302, 3763, 1306, 3760);
	Area mountkaruulstash = new Area(1302, 3845, 1312, 3834);
	Area mountkaruulstashsmall = new Area(1307, 3840, 1308, 3839);
	Area lighthouseto7292digspot = new Area(2490, 3654, 2613, 3577);
	Area c7292digspot = new Area(2578, 3597, 2578, 3597);
	Area c7305digspot = new Area(2537, 3881, 2537, 3881);
	Area cipto7305digspot = new Area(2488, 3907, 2580, 3837);
	Area cipteleport = new Area(2510, 3888, 2516, 3883);
	Area karamjato3588digspot = new Area(2931, 3135, 2870, 3185);
	Area c3588digspot = new Area(2888, 3153, 2888, 3153);
	Area fairyrintoshilomine = new Area(2776, 3055, 2882, 2983);
	Area CKRteleport = new Area(2797, 3009, 2806, 3000);
	Area shilominedigspot = new Area(2849, 3033, 2849, 3033);
	Area c12039digspot = new Area(2680, 3652, 2680, 3652);
	Area taibwowannaifencemiddle = new Area(2783, 3071, 2803, 3058);
	Area taibwowannaifencemiddlesmall = new Area(2790, 3061, 2791, 3060);
	Area akstofeldiphills2 = new Area(2531, 3010, 2643, 2891);
	Area c7307digspot = new Area(2583, 2990, 2583, 2990);
	Area lumbridgeswampcave = new Area(3135, 9603, 3265, 9533);
	Area lumbridgegraveyardtoswamp = new Area(3143, 3217, 3255, 3142);
	Area lumbyswampcaveentrancesmall = new Area(3168, 3173, 3169, 3172);
	Area woodcuttingguildtohosidiusvinery = new Area(1607, 3598, 1879, 3455);
	Area northvineryhosidius = new Area(new Tile(1800, 3565),new Tile(1802, 3565),new Tile(1805, 3568),new Tile(1811, 3568),new Tile(1814, 3565),new Tile(1816, 3565),new Tile(1816, 3554),new Tile(1800, 3554));
	Area northvineryhosidiussmall = new Area(1806, 3562, 1807, 3561);
	Area alkharidtokebabshop = new Area(3258, 3199, 3327, 3116);
	Area alkharidkebabshop = new Area(3271, 3183, 3275, 3179);
	Area alkharidlargedoor = new Area(3292, 3166, 3293, 3166);
	Area alkharidtproom = new Area(3282, 3166, 3303, 3159);
	Area alkharidkebabshopsmall = new Area(3271, 3181, 3272, 3180);
	Area catherbytocamelotbeach = new Area(2729, 3524, 2887, 3391);
	Area camelotbeachstash = new Area(2833, 3439, 2840, 3433);
	Area camelotbeachstashsmall = new Area(2836, 3436, 2837, 3435);
	Area camelotbeachsmall = new Area(2854, 3425, 2855, 3424);
	Area camelotbeach = new Area(2848, 3426, 2858, 3422);
	Area varrocktosouthentrance = new Area(3194, 3445, 3230, 3372);
	Area varrocksouthentrance = new Area(3202, 3397, 3219, 3382);
	Area varrocksouthentrancesmall = new Area(3208, 3390, 3209, 3389);
	Area portpascariliusbigger = new Area(1736, 3820, 1868, 3668);
	Area nicholasbig = new Area(1831, 3821, 1853, 3799);
	Area nicholassmall = new Area(1844, 3811, 1845, 3810);
	Area camelottocamelotcastle = new Area(2735, 3524, 2791, 3455);
	Area camelotcastlecourtyard = new Area(2752, 3503, 2764, 3493);
	Area camelotcastlecourtyardsmall = new Area(2757, 3501, 2758, 3500);
	Area alkharidtoshantaypass = new Area(3266, 3197, 3335, 3109);
	Area shantaypass = new Area(3292, 3133, 3316, 3117);
	Area shantaypassstash = new Area(3306, 3128, 3307, 3127);
	Area shantaypassmiddle = new Area(3301, 3126, 3305, 3122);
	Area shantaypassmiddlesmall = new Area(3302, 3125, 3303, 3124);
	Area faladortofaladorcastle = new Area(2944, 3393, 3007, 3322);
	Area faladortp = new Area(2958, 3387, 2972, 3373);
	Area faladorcastlesmall = new Area(2979, 3341, 2980, 3340);
	Area faladorcastle = new Area(2967, 3348, 2981, 3337);
	Area fishingguildtoedmond = new Area(2550, 3411, 2632, 3281);
	Area fishingguildtp = new Area(2602, 3397, 2619, 3386);
	Area edmondshousesmall = new Area(2564, 3333, 2565, 3332);
	Area edmondshouse = new Area(new Tile(2565, 3338),new Tile(2568, 3338),new Tile(2569, 3339),new Tile(2580, 3339),new Tile(2580, 3329),new Tile(2564, 3329),new Tile(2563, 3330),new Tile(2563, 3336));
	Area taibwowannaibrokenjunglefence = new Area(2795, 3080, 2807, 3071);
	Area taibwowannaibrokenjunglefencesmall = new Area(2799, 3074, 2800, 3073);
	Area c3604crate = new Area(2800, 3074, 2800, 3074);
	Area ardougnetoardougnepub = new Area(2551, 3354, 2682, 3272);
	Area ardougnepubupstairs = new Area(new Tile(2562, 3329, 1), new Tile(2579, 3316, 1));
	Area handelmortmansion = new Area(new Tile(2627, 3332),new Tile(2643, 3332),new Tile(2646, 3329),new Tile(2647, 3329),new Tile(2647, 3319),new Tile(2643, 3315),new Tile(2643, 3309),new Tile(2642, 3308),new Tile(2627, 3308),new Tile(2626, 3309),new Tile(2626, 3314),new Tile(2625, 3315),new Tile(2624, 3315),new Tile(2624, 3329));
	Area handelmortmansionsmall = new Area(2634, 3318, 2635, 3317);
	Area ardougnepub = new Area(new Tile(2572, 3327),new Tile(2577, 3327),new Tile(2578, 3326),new Tile(2578, 3324),new Tile(2577, 3323),new Tile(2577, 3322),new Tile(2576, 3322),new Tile(2576, 3318),new Tile(2572, 3318));
	Area ardougnepubsmall = new Area(2572, 3324, 2573, 3323);
	Area drawersc2833 = new Area(new Tile(2574, 3326, 1), new Tile(2574, 3326, 1));
	Area ardougnetowitchaven = new Area(2637, 3330, 2747, 3268);
	Area witchavennorth = new Area(2710, 3308, 2724, 3296);
	Area witchavennorthsmall = new Area(2718, 3302, 2719, 3301);
	Area craftingguildtohobgobisland = new Area(2894, 3304, 2952, 3250);
	Area craftingguildtp = new Area(2927, 3299, 2941, 3286);
	Area c3596digtile = new Area(2906, 3293, 2906, 3293);
	Area brimhavenporttoshrimpandparrot = new Area(2745, 3248, 2815, 3148);
	Area brimhavenshrimpandparrot = new Area(2787, 3189, 2800, 3181);
	Area brimhavenshrimpandparrotsmall = new Area(2793, 3186, 2794, 3185);
	Area hazelmerehousez1 = new Area(new Tile(2672, 3091, 1), new Tile(2682, 3083, 1));
	Area hazelmerehousez0 = new Area(new Tile(2676, 3089),new Tile(2679, 3089),new Tile(2680, 3088),new Tile(2680, 3087),new Tile(2679, 3086),new Tile(2676, 3086),new Tile(2675, 3087),new Tile(2675, 3088));
	Area hazelmerehousez0small = new Area(2676, 3087, 2677, 3086);
	Area mortmyretoc3584digspot = new Area(3396, 3454, 3533, 3257);
	Area c3584digspot = new Area(3430, 3388, 3430, 3388);
	Area logbalancec12049 = new Area(2602, 3477, 2602, 3477);
	Area beforec12049digspot = new Area(2604, 3477, 2605, 3476);
	Area c12049digspot = new Area(2585, 3505, 2585, 3505);
	Area rangingguildtoc12049digspot = new Area(2554, 3521, 2684, 3409);
	Area wizardtowerbasementdrawer = new Area(3116, 9562, 3116, 9562);
	Area wizardtowerbasement = new Area(3092, 9583, 3124, 9549);
	Area wizardtowerbasementdrawerbig = new Area(3111, 9567, 3119, 9555);
	Area wizardtowerbasementdrawersmall = new Area(3114, 9562, 3115, 9561);
	Area wizardtowerbeforestaircaseroombig = new Area(3100, 3168, 3119, 3152);
	Area wizardtowerbeforestaircaseroomsmall = new Area(3108, 3163, 3109, 3162);
	Area frontdoorwizardtower = new Area(3109, 3167, 3109, 3167);
	Area treegnomestrongholdtonieve = new Area(2413, 3461, 2473, 3401);
	Area nievegnomestrongholdsmall = new Area(2431, 3424, 2432, 3423);
	Area nievegnomestronghold = new Area(2426, 3428, 2437, 3417);
	Area cipto7286digspot = new Area(2493, 3913, 2592, 3833);
	Area c7286digspot = new Area(2536, 3865, 2536, 3865);
	Area insiderantzcave = new Area(2630, 9404, 2666, 9373);
	Area akstorantzcave = new Area(2524, 3011, 2662, 2883);
	Area rantzcaveentrace = new Area(2622, 3004, 2641, 2989);
	Area rantzcaveentrancesmall = new Area(2630, 2996, 2631, 2995);
	Area rantzcavemiddle = new Area(2633, 9399, 2659, 9388);
	Area rantzcavemiddlesmall = new Area(2645, 9397, 2646, 9396);
	Area bugzone = new Area(2605, 2998, 2651, 2963);
	Area c7315digspot = new Area(2735, 3638, 2735, 3638);
	Area goldenappletreetoc7315digspot = new Area(2607, 3697, 2813, 3575);
	Area goldenappletree = new Area(2751, 3629, 2790, 3589);
	Area outposttotrainingcamp = new Area(2412, 3393, 2544, 3325);
	Area trainingcampogrepen = new Area(new Tile(2521, 3378),new Tile(2534, 3378),new Tile(2534, 3369),new Tile(2523, 3369),new Tile(2521, 3371));
	Area trainingcampogrepensmall = new Area(2521, 3376, 2522, 3375);
	Area trainingcampogrepeninside = new Area(2523, 3377, 2533, 3373);
	Area faladortoc2821digspot = new Area(2872, 3490, 3007, 3340);
	Area digspot2821 = new Area(2920, 3404, 2920, 3404);
	Area lumbridgegraveyardtoironman = new Area(3196, 3241, 3258, 3171);
	Area ironmantutor = new Area(3224, 3230, 3235, 3221);
	Area ironmantutorsmall = new Area(3228, 3228, 3229, 3227);
	Area draynorvillagetojail = new Area(3069, 3287, 3141, 3211);
	Area draynorjailoutsidesmall = new Area(3129, 3250, 3130, 3249);
	Area draynorjailoutside = new Area(3124, 3252, 3131, 3247);
	Area draynorjailinsidesmall = new Area(3127, 3244, 3128, 3243);
	Area draynorjailinside = new Area(3121, 3246, 3130, 3240);
	Area dksto7309digspot = new Area(2876, 3136, 2930, 3085);
	Area c7309digspot = new Area(2896, 3119, 2896, 3119);
	Area sinclaurmansionupstairsdebug = new Area(new Tile(2740, 3583, 1), new Tile(2748, 3572, 1));
	Area lumbridgegraveyardtochurch = new Area(3198, 3240, 3262, 3168);
	Area lumbridgechurch = new Area(new Tile(3240, 3216),new Tile(3248, 3216),new Tile(3248, 3204),new Tile(3240, 3204),new Tile(3240, 3209),new Tile(3238, 3209),new Tile(3238, 3212),new Tile(3240, 3212));
	Area lumbridgechurchsmall = new Area(3244, 3210, 3245, 3209);
	Area treegnomestrongholdto3594 = new Area(2371, 3538, 2497, 3394);
	Area digspot3594 = new Area(2416, 3515, 2416, 3515);
	Area seaslugplatform = new Area(2757, 3295, 2798, 3269);
	Area witchavennorth2 = new Area(2716, 3311, 2729, 3296);
	Area witchavennorth2small = new Area(2721, 3304, 2722, 3303);
	Area seaslugplatformhouse = new Area(new Tile(2762, 3278),new Tile(2763, 3279),new Tile(2767, 3279),new Tile(2768, 3278),new Tile(2768, 3274),new Tile(2767, 3273),new Tile(2763, 3273),new Tile(2762, 3274));
	Area seaslugplatformhousesmall = new Area(2763, 3275, 2764, 3274);
	Area monasterytomountain = new Area(2987, 3520, 3115, 3410);
	Area monasterytp = new Area(3043, 3500, 3060, 3481);
	Area whitewolfmountaintop = new Area(3001, 3510, 3021, 3494);
	Area whitewolfmountaintopsmall = new Area(3012, 3503, 3013, 3502);
	Area catherbybankoutside = new Area(new Tile(2802, 3440),new Tile(2806, 3440),new Tile(2806, 3438),new Tile(2811, 3438),new Tile(2811, 3434),new Tile(2802, 3434));
	Area catherbybankoutsidesmall = new Area(2806, 3437, 2807, 3436);
	Area catherbybankinside = new Area(2806, 3445, 2812, 3438);
	Area catherbybankinsidesmall = new Area(2808, 3441, 2809, 3440);
	Area westardougnetosquare = new Area(2464, 3333, 2557, 3265);
	Area westardougneteleport = new Area(2494, 3298, 2514, 3280);
	Area westardougnesquare = new Area(2536, 3311, 2547, 3296);
	Area westardougnesquaresmall = new Area(2543, 3307, 2544, 3306);
	Area catherbytocamelot2 = new Area(2733, 3501, 2845, 3406);
	Area catherbyranging = new Area(2821, 3445, 2825, 3441);
	Area catherbyrangingsmall = new Area(2823, 3443, 2824, 3442);
	Area bkpto12041digspot = new Area(2310, 3088, 2430, 3017);
	Area bkpteleport = new Area(2383, 3039, 2388, 3034);
	Area c12041digspot = new Area(2322, 3060, 2322, 3060);
	Area treegnomestrongholdto10266 = new Area(2441, 3458, 2493, 3404);
	Area gnomeagilityarenaz0 = new Area(2469, 3432, 2482, 3418);
	Area gnomeagilityarenaz1 = new Area(new Tile(2469, 3425, 1), new Tile(2477, 3421, 1));
	Area gnomeagilityarenaz2 = new Area(new Tile(2467, 3428, 2), new Tile(2484, 3412, 2));
	Area obstaclenetsagility = new Area(2471, 3425, 2475, 3425);
	Area gnomeagilityarenaz0small = new Area(2473, 3426, 2474, 3425);
	Area burthorpecastlez1 = new Area(new Tile(2889, 3573, 1), new Tile(2909, 3553, 1));
	Area burthorpetocastlez0 = new Area(2879, 3581, 2922, 3524);
	Area insideburthorpecastlez0 = new Area(new Tile(2891, 3572),new Tile(2896, 3572),new Tile(2896, 3570),new Tile(2902, 3570),new Tile(2902, 3572),new Tile(2907, 3572),new Tile(2907, 3567),new Tile(2905, 3567),new Tile(2905, 3561),new Tile(2907, 3561),new Tile(2907, 3556),new Tile(2902, 3556),new Tile(2902, 3558),new Tile(2900, 3558),new Tile(2900, 3559),new Tile(2898, 3559),new Tile(2898, 3558),new Tile(2896, 3558),new Tile(2896, 3556),new Tile(2891, 3556),new Tile(2891, 3561),new Tile(2893, 3561),new Tile(2893, 3567),new Tile(2891, 3567));
	Area burthorpecastlestairs = new Area(2897, 3565, 2898, 3564);
	Area tileinfrontofdoor = new Area(2898, 3558, 2899, 3558);
	Area burthorpecastledoor = new Area(2898, 3558, 2899, 3558);
	Area faladortopartyroom = new Area(2938, 3398, 3067, 3338);
	Area partyroom = new Area(new Tile(3037, 3386),new Tile(3040, 3386),new Tile(3041, 3385),new Tile(3051, 3385),new Tile(3052, 3386),new Tile(3055, 3386),new Tile(3056, 3385),new Tile(3056, 3382),new Tile(3055, 3381),new Tile(3055, 3376),new Tile(3056, 3375),new Tile(3056, 3372),new Tile(3055, 3371),new Tile(3052, 3371),new Tile(3051, 3372),new Tile(3041, 3372),new Tile(3040, 3371),new Tile(3037, 3371),new Tile(3036, 3372),new Tile(3036, 3375),new Tile(3037, 3376),new Tile(3037, 3381),new Tile(3036, 3382),new Tile(3036, 3385));
	Area faladorcastledoor = new Area(3045, 3371, 3046, 3371);
	Area partyroomsmall = new Area(3044, 3378, 3045, 3377);
	Area onthewaytopartyroom = new Area(3020, 3362, 3020, 3362);
	Area C2815digspot = new Area(2848, 3297, 2848, 3297);
	Area c2807digspot = new Area(2383, 3369, 2383, 3369);
	Area outpostto2807digspot = new Area(2352, 3386, 2454, 3325);
	Area lumbridgegraveyardtoswamp2 = new Area(3136, 3212, 3255, 3134);
	Area c7313digspot = new Area(3183, 3151, 3183, 3151);
	Area c2823digspot = new Area(3217, 3178, 3217, 3178);
	Area treegnomestrongholdtoswcave = new Area(2363, 3508, 2507, 3384);
	Area strongholdswcaveentrance = new Area(2393, 3426, 2413, 3408);
	Area strongholdswcaveentrancesmall = new Area(2401, 3419, 2402, 3418);
	Area insideswcave = new Area(2376, 9836, 2417, 9805);
	Area faladortoinn = new Area(2946, 3392, 2984, 3361);
	Area risingsuninn = new Area(new Tile(2955, 3379),new Tile(2958, 3379),new Tile(2958, 3376),new Tile(2961, 3376),new Tile(2961, 3374),new Tile(2962, 3374),new Tile(2962, 3369),new Tile(2961, 3369),new Tile(2961, 3367),new Tile(2960, 3366),new Tile(2954, 3366),new Tile(2953, 3367),new Tile(2953, 3375),new Tile(2955, 3375));
	Area risingsuninnsmall = new Area(2955, 3370, 2956, 3369);
	Area varrockcenterlarge = new Area(3195, 3444, 3229, 3416);
	Area bareakarea = new Area(3211, 3438, 3223, 3428);
	Area bareakareasmall = new Area(3218, 3434, 3219, 3433);
	Area djrtoc19774digspot = new Area(1397, 3688, 1487, 3575);
	Area djrteleport = new Area(1450, 3662, 1456, 3656);
	Area c19774digspot = new Area(1456, 3620, 1456, 3620);
	Area digspot7290 = new Area(2455, 3230, 2455, 3230);
	Area battlefieldofkhazadtoouraniacave = new Area(new Tile(2418, 3279),new Tile(2510, 3279),new Tile(2510, 3264),new Tile(2558, 3264),new Tile(2558, 3277),new Tile(2592, 3277),new Tile(2593, 3183),new Tile(2417, 3181));
	Area khazadspirittree = new Area(2548, 3263, 2561, 3252);
	Area pohtodesertmine = new Area(3242, 3062, 3365, 2984);
	Area desertminejail = new Area(3284, 3037, 3287, 3031);
	Area desertmineentrance = new Area(3261, 3036, 3273, 3022);
	Area desertmineentrancesmall = new Area(3271, 3029, 3272, 3028);
	Area desertmineinside = new Area(3274, 3042, 3305, 3012);
	Area desertminecrate = new Area(3283, 3027, 3293, 3018);
	Area desertminecratesmall = new Area(3287, 3021, 3288, 3020);
	Area desertminecratetile = new Area(3289, 3022, 3289, 3022);
	Area arceuuslibrarymiddle = new Area(1627, 3813, 1638, 3802);
	Area arceuuslibrarymiddlesmall = new Area(1632, 3808, 1633, 3807);
	Area arceuuslibrarystash = new Area(1634, 3817, 1642, 3803);
	Area arceuuslibrarystashsmall = new Area(1640, 3810, 1641, 3809);
	Area yanilleinsidelargehousez1 = new Area(new Tile(2590, 3108, 1), new Tile(2598, 3103, 1));
	Area yanillefairytobank = new Area(2494, 3147, 2625, 3066);
	Area nwayanilletp = new Area(2525, 3131, 2531, 3126);
	Area yanilleoutsidelargehouse = new Area(new Tile(2585, 3108),new Tile(2590, 3108),new Tile(2590, 3103),new Tile(2599, 3103),new Tile(2599, 3109),new Tile(2608, 3109),new Tile(2608, 3095),new Tile(2585, 3095));
	Area yanilleoutsidelargehousesmall = new Area(2598, 3102, 2599, 3101);
	Area yanilleoutside = new Area(2524, 3100, 2532, 3082);
	Area yanilleairlock = new Area(2533, 3093, 2538, 3090);
	Area yanillefirstdoor = new Area(2532, 3092, 2532, 3091);
	Area yanilleseconddoor = new Area(2539, 3092, 2539, 3091);
	Area yanillethirddoor = new Area(2594, 3102, 2594, 3102);
	Area yanilleinside = new Area(new Tile(2543, 3109),new Tile(2539, 3107),new Tile(2539, 3077),new Tile(2543, 3075),new Tile(2582, 3075),new Tile(2584, 3073),new Tile(2589, 3073),new Tile(2591, 3075),new Tile(2619, 3075),new Tile(2620, 3076),new Tile(2620, 3097),new Tile(2608, 3109));
	Area yanilleinsidelargehouse = new Area(2590, 3108, 2598, 3103);
	Area yanilleinsidelargehousesmall = new Area(2595, 3107, 2596, 3106);
	Area onthewaytopartyroom2 = new Area(3039, 3369, 3039, 3369);
	Area c12037digspot = new Area (3548, 3560, 3548, 3560);
	Area fenkenstrainscastletoc12037 = new Area(3530, 3572, 3570, 3507);
	Area c3586digspot = new Area(2920, 3534, 2920, 3534);
	Area burthorpeto3586digspot = new Area(2881, 3560, 2936, 3519);
	Area burthorpetoinntodunstan = new Area(2879, 3581, 2946, 3513);
	Area burthorpeinn = new Area(2905, 3543, 2915, 3536);
	Area burthorpeinnsmall = new Area(2910, 3539, 2911, 3538);
	Area dunstanshouseroom = new Area(2921, 3577, 2923, 3575);
	Area dunstanshouseroomsmall = new Area(2921, 3577, 2922, 3576);
	Area rockareabaxtorianfalls = new Area(2512, 3468, 2512, 3468);
	Area c2811digtile = new Area(2512, 3466, 2512, 3466);
	Area barbarianoutposttobaxtorian = new Area(2471, 3605, 2561, 3457);
	Area baxtorianisland2 = new Area(2508, 3470, 2515, 3462);
	Area baxtorianisland1 = new Area(2509, 3482, 2514, 3475);
	Area raftarea = new Area(2508, 3498, 2512, 3490);
	Area raftareasmall = new Area(2509, 3494, 2510, 3493);
	Area karamjatoluthas = new Area(2884, 3187, 2962, 3129);
	Area luthashouse = new Area(2935, 3156, 2941, 3152);
	Area luthashousesmall = new Area(2938, 3155, 2939, 3154);
	Area fairyrintoshilovines = new Area(2753, 3098, 2885, 2958);
	Area shilovinesdigspot = new Area(2874, 3047, 2874, 3047);
	Area arceuuslibrarygrackle = new Area(1622, 3806, 1633, 3797);
	Area arceuuslibrarygracklesmall = new Area(1624, 3800, 1625, 3799);
	Area yanillelargehouse = new Area(2583, 3108, 2604, 3098);
	Area burthorpetodunstan = new Area(2877, 3585, 2938, 3526);
	Area dunstanshouselarge = new Area(2917, 3577, 2923, 3572);
	Area dunstanshouselargesmall = new Area(2919, 3575, 2920, 3574);
	Area cjrto7294digspot = new Area(2629, 3596, 2761, 3518);
	Area c7294digspot = new Area(2666, 3562, 2666, 3562);
	Area piscatoristo12047digtile = new Area(2255, 3650, 2392, 3503);
	Area kandarinpiscatoristp = new Area(2316, 3619, 2322, 3614);
	Area c12047digtile = new Area(2362, 3531, 2362, 3531);
	Area draynormanorto2825digspot = new Area(3074, 3368, 3223, 3278);
	Area digspot2825 = new Area(3179, 3343, 3179, 3343);
	Area corsaircovetospa = new Area(2471, 2875, 2608, 2831);
	Area corsaircovespirittree = new Area(2481, 2855, 2498, 2840);
	Area spaladybig = new Area(2550, 2873, 2563, 2863);
	Area spaladysmall = new Area(2553, 2869, 2554, 2868);
	Area drezelcaveinside = new Area(3431, 9905, 3443, 9882);
	Area CKStodrezelcave = new Area(3400, 3530, 3522, 3453);
	Area drezelcaveentrance = new Area(3421, 3486, 3432, 3483);
	Area drezelcaveentrancesmall = new Area(3422, 3485, 3423, 3484);
	Area drezelareasmall = new Area(3439, 9899, 3440, 9898);
	Area drezelarea = new Area(3436, 9901, 3443, 9893);
	Area crateinardougnemonasterybig = new Area(new Tile(2611, 3207),new Tile(2614, 3204),new Tile(2618, 3204),new Tile(2622, 3208),new Tile(2622, 3211),new Tile(2618, 3215),new Tile(2614, 3215),new Tile(2611, 3212));
	Area crateinardougnemonasterysmall = new Area(2615, 3205, 2616, 3204);
	Area crateardougnemonastery = new Area(2614, 3204, 2614, 3204);
	Area c2813digspot = new Area(2644, 3251, 2644, 3251);
	Area toweroflifeto2813digspot = new Area(2580, 3291, 2691, 3190);
	Area tilebeforetrapdoor = new Area(3422, 3485, 3423, 3484);
	Area yanilletobank = new Area(2490, 3142, 2689, 3061);
	Area yanilleinfrontofbank = new Area(2597, 3099, 2608, 3086);
	Area yanilleinfrontofbanksmall = new Area(2603, 3092, 2604, 3091);
	Area yanilleinsidebank = new Area(2609, 3097, 2616, 3088);
	Area yanilleinsidebanksmall = new Area(2611, 3093, 2612, 3092);
	Area infrontoflargedoor2 = new Area(2532, 3093, 2532, 3090);
	Area infrontoflargedoor = new Area(2538, 3093, 2538, 3090);
	Area yanillelargehousedoor = new Area(2593, 3102, 2595, 3102);
	Area infrontoffrontdoorwizardtower = new Area(3108, 3167, 3110, 3167);
	Area farmingguildtp = new Area(1245, 3731, 1252, 3723);
	Area farmingguild = new Area(1274, 3720, 1220, 3765);
	Area c23135digspot = new Area(1247, 3726, 1247, 3726);
	Area edgevilletogeneralstore = new Area(3072, 3516, 3113, 3462);
	Area edgevillestashunitsmall = new Area(3077, 3503, 3078, 3502);
	Area edgevillestashunit = new Area(3072, 3506, 3083, 3499);
	Area edgevillegeneralstoresmall = new Area(3079, 3511, 3080, 3510);
	Area edgevillegeneralstore = new Area(new Tile(3077, 3514),new Tile(3076, 3513),new Tile(3076, 3508),new Tile(3077, 3507),new Tile(3084, 3507),new Tile(3085, 3508),new Tile(3085, 3513),new Tile(3084, 3514));
	Area edgevilletobarbbridge = new Area(3041, 3519, 3136, 3401);
	Area barbbridgestash = new Area(3108, 3424, 3113, 3416);
	Area barbbridgestashsmall = new Area(3109, 3422, 3110, 3421);
	Area barbbridgesmall = new Area(3104, 3421, 3105, 3420);
	Area barbbridge = new Area(3103, 3421, 3107, 3420);
	Area toweroflifeto3599digspot = new Area(2625, 3259, 2685, 3202);
	Area c3599digspot = new Area(2651, 3230, 2651, 3230);
	Area stucktileburthorpe = new Area(2915, 3537, 2915, 3537);
	Area unstucktileburthorpe = new Area(2910, 3537, 2910, 3537);
	Area digsitetoboat = new Area(3296, 3472, 3404, 3363);
	Area digsiteboat = new Area(3353, 3450, 3365, 3442);
	Area digsiteboatsmall = new Area(3361, 3445, 3362, 3444);
	Area southfossilisland = new Area(3714, 3815, 3736, 3798);
	Area northfossilisland = new Area(3721, 3908, 3750, 3878);
	Area outatseafossilisland = new Area(3760, 3907, 3780, 3892);
	Area c23137digspot = new Area(3770, 3898, 3770, 3898);
	Area c7311digspot = new Area(3005, 3475, 3005, 3475);
	Area mudskipperto2819digspot = new Area(2968, 3185, 3040, 3095);
	Area mudskipperteleport = new Area(2993, 3115, 2999, 3108);
	Area c2819digspot = new Area(3007, 3145, 3007, 3145);
	Area barbarianoutposttoottosgrotto = new Area(new Tile(2488, 3552),new Tile(2486, 3474),new Tile(2567, 3476),new Tile(2564, 3542),new Tile(2529, 3542),new Tile(2529, 3551),new Tile(2528, 3552),new Tile(2528, 3557),new Tile(2545, 3557),new Tile(2546, 3558),new Tile(2546, 3597),new Tile(2489, 3596));
	Area ottosgrotto = new Area(2500, 3490, 2503, 3487);
	Area ottosgrottosmall = new Area(2500, 3488, 2501, 3487);
	Area digsitetoemoteclue = new Area(3304, 3451, 3386, 3370);
	Area digsitestashsmall = new Area(3370, 3420, 3371, 3419);
	Area digsitestash = new Area(3365, 3422, 3374, 3418);
	Area digsitewell = new Area(3367, 3429, 3372, 3424);
	Area digsitewellsmall = new Area(3369, 3427, 3370, 3426);
	Area canifisstashunit = new Area(3487, 3493, 3497, 3483);
	Area canifisstashunitsmall = new Area(3490, 3490, 3491, 3489);
	/** END OF CLUE AREAS TODO **/
	
	/***
	currentClue = 10254; //MAKE SURE TO CHECKDIS
	currentCluestr = Integer.toString(currentClue);
	
	if (previousClue != currentClue) {
		if (Walking.isRunEnabled() == false) {
			Walking.toggleRun();
		}
		
		if (Combat.isAutoRetaliateOn() == true) {
			sleep(randomNum(100,300));
	        Combat.toggleAutoRetaliate(false);
	        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
			sleep(randomNum(100,300));
		}
		
		dbbotcluestotal ++;
		timeBeganClue = System.currentTimeMillis();
		
		dbbotworld = Client.getCurrentWorld();
		dbbottask = "Clue "+currentCluestr;
		dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
		onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
	}
	
	//TASK
	
	if (Inventory.contains("Reward casket (medium)")) {
		setupcomplete = 0;
		sleep(randomNum(300,600));
	} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
		log("stuck on clue: " + currentClue);
		if (Inventory.contains("Clue scroll (medium)")) {
			sleep(randomNum(400,700));
			Inventory.drop("Clue scroll (medium)");
			sleep(randomNum(300,600));
		}
		backing = 1;
		setupcomplete = 0;
	}
	
	previousClue = currentClue;
	***/
	
	@Override //Infinite loop
	public int onLoop() {

		//Determined by which state gets returned by getState() then do that case.
		switch(getState()) {
		
		case C10254: //Dance in the centre of Canifis. Bow before you talk to me. EMOTE
			C10254();
			break;

		case C10256: //Panic by the mausoleum in Morytania. Wave before you speak to me. EMOTE
			C10256();
			break;

		case C10258: //Spin on the bridge by the Barbarian Village. Salute before you talk to me. EMOTE
			C10258();
			break;

		case C10260: //Beckon in Tai Bwo Wannai. Clap before you talk to me. EMOTE
			C10260();
			break;

		case C10262: //Yawn in the Castle Wars lobby. Shrug before you talk to me. EMOTE
			C10262();
			break;

		case C10264: //Cheer in the Barbarian Agility Arena. Headbang before you talk to me. EMOTE
			C10264();
			break;

		case C10266: //Cry on top of the western tree in the Gnome Agility Arena. Indicate 'no' before you talk to me. EMOTE
			C10266();
			break;

		case C10268: //Jump for joy in Yanille bank. Dance a jig before you talk to me. EMOTE
			C10268();
			break;

		case C10270: //Think in the centre of the Observatory. Spin before you talk to me. EMOTE
			C10270();
			break;

		case C10272: //Cheer in the Ogre Pen in the Training Camp. Show you are angry before you talk to me. EMOTE
			C10272();
			break;

		case C10274: //Beckon in the Digsite, near the eastern winch. Bow before you talk to me. EMOTE
			C10274();
			break;

		case C10276: //Cry in the Catherby Ranging shop. Bow before you talk to me. EMOTE
			C10276();
			break;

		case C10278: //Dance a jig under Shantay's Awning. Bow before you talk to me. EMOTE
			C10278();
			break;

		case C12021: //Dance in the dark caves beneath Limbridge Swamp. Blow a kiss before you talk to me. EMOTE
			C12021();
			break;

		case C12023: //Shrug in Catherby bank. Yawn before you talk to me. EMOTE
			C12023();
			break;

		case C12025: //Clap in Seers court house. Spin before you talk to me. EMOTE
			C12025();
			break;

		case C12027: //Cry on the shore of Catherby beach. Laugh before you talk to me. EMOTE
			C12027();
			break;

		case C12029: //Jump for joy in the TzHaar sword shop. Shrug before you talk to me. EMOTE
			C12029();
			break;

		case C12031: //Cheer in the Edgeville general store. Dance before you talk to me. EMOTE
			C12031();
			break;

		case C12033: //08.11S, 04.48E COORDINATE
			C12033();
			break;

		case C12035: //02.43S, 33.26E COORDINATE
			C12035();
			break;

		case C12037: //12.28N, 34.37E COORDINATE
			C12037();
			break;

		case C12039: //15.22N, 07.31E COORDINATE
			C12039();
			break;

		case C12041: //03.07S, 03.41W COORDINATE
			C12041();
			break;

		case C12043: //06.58N, 21.16E COORDINATE
			C12043();
			break;

		case C12045: //09.35N, 01.50W COORDINATE
			C12045();
			break;

		case C12047: //11.33N, 02.24W COORDINATE
			C12047();
			break;

		case C12049: //10.45N, 04.31E COORDINATE
			C12049();
			break;

		case C12051: //06.41N, 27.15E COORDINATE
			C12051();
			break;

		case C12053: //11.18N, 30.54E COORDINATE
			C12053();
			break;

		case C12055: //GOBLETS ODD TOES ANAGRAM
			C12055();
			break;

		case C12057: //A BAKER ANAGRAM
			C12057();
			break;

		case C12059: //I EVEN ANAGRAM
			C12059();
			break;

		case C12061: //A BASIC ANTI POT ANAGRAM
			C12061();
			break;

		case C12063: //RATAI ANAGRAM
			C12063();
			break;

		case C12065: //LEAKEY ANAGRAM
			C12065();
			break;

		case C12067: //THICKNO ANAGRAM
			C12067();
			break;

		case C12069: //KAY SIR ANAGRAM
			C12069();
			break;

		case C12071: //HEORIC ANAGRAM
			C12071();
			break;

		case C19734: //PACINNG A TAIE ANAGRAM
			C19734();
			break;

		case C19736: //I DOOM ICON INN ANAGRAM
			C19736();
			break;

		case C19738: //LOW LAG ANAGRAM
			C19738();
			break;

		case C19740: //R SLICER ANAGRAM
			C19740();
			break;

		case C19742: //HIS PHOR ANAGRAM
			C19742();
			break;

		case C19744: //TAMED ROCKS ANAGRAM
			C19744();
			break;

		case C19746: //AREA CHEF TREK ANAGRAM
			C19746();
			break;

		case C19748: //SAND NUT ANAGRAM
			C19748();
			break;

		case C19750: //ARMCHAIR THE PELT ANAGRAM
			C19750();
			break;

		case C19752: //PEAK REFLEX ANAGRAM
			C19752();
			break;

		case C19754: //QUE SIR ANAGRAM
			C19754();
			break;

		case C19756: //I AM SIR ANAGRAM
			C19756();
			break;

		case C19758: //A HEART ANAGRAM
			C19758();
			break;

		case C19760:
			break;

		case C19762: //QSPGFTTPS HSBDLMFCPOF CIPHER
			C19762();
			break;

		case C19764: //USBJCPSO CIPHER
			C19764();
			break;

		case C19766: //ECRUCKP MJCNGF CIPHER
			C19766();
			break;

		case C19768: //BMJ UIF LFCBC TFMMFS CIPHER
			C19768();
			break;

		case C19770: //HQNM LZM STSNQ CIPHER
			C19770();
			break;

		case C19772: //GUHCHO CIPHER
			C19772();
			break;

		case C19774: //14.20N, 30.45W COORDINATE
			C19774();
			break;

		case C19776: //Beckon in the combat ring of shayzien. Show your anger before you talk to me. EMOTE
			C19776();
			break;

		case C19778: //Yawn in the centre of Arceuus library. Nod your head before you talk to me. EMOTE
			C19778();
			break;

		case C19780: //Cry in the Draynor village jail. Jump for joy before you talk to me. EMOTE
			C19780();
			break;

		case C23046: //Clap your hands north of Mount Karuulm. Spin before you talk to me. EMOTE 
			C23046();
			break;

		case C23131: //CLASH ION ANAGRAM
			C23131();
			break;

		case C23133: //CALAMARI MADE MUD ANAGRAM
			C23133();
			break;

		case C23135: //17.39N, 37.16W COORDINATE
			C23135();
			break;

		case C23136: //2.16N, 12.7E COORDINATE
			C23136();
			break;

		case C23137: //23.01N, 41.33E COORDINATE
			C23137();
			break;

		case C23138: //Karamja Jam MUSIC
			C23138();
			break;

		case C23139: //Faerie MUSIC
			C23139();
			break;

		case C23140: //Forgotten MUSIC
			C23140();
			break;

		case C23141: //Catch Me If You Can MUSIC
			C23141();
			break;

		case C23142: //Cave of Beasts MUSIC
			C23142();
			break;

		case C23143: //Devils May Care MUSIC
			C23143();
			break;

		case C2801: //02.48N, 22.30E COORDINATE
			C2801();
			break;

		case C2803: //01.35S, 07.28E COORDINATE
			C2803();
			break;

		case C2805: //01.26N, 08.01E COORDINATE
			C2805();
			break;

		case C2807: //06.31N, 01.46W COORDINATE
			C2807();
			break;

		case C2809: //00.05S, 01.13E COORDINATE
			C2809();
			break;

		case C2811: //09.33N, 02.15E COORDINATE
			C2811();
			break;

		case C2813: //02.50N, 06.20E COORDINATE
			C2813();
			break;

		case C2815: //04.13N, 12.45E COORDINATE
			C2815();
			break;

		case C2817: //04.00S, 12.46E COORDINATE
			C2817();
			break;

		case C2819: //00.31S, 17.43E COORDINATE
			C2819();
			break;

		case C2821: //07.33N, 15.00E COORDINATE
			C2821();
			break;

		case C2823: //00.30N, 24.16 COORDINATE
			C2823();
			break;

		case C2825: //05.43N, 23.05E COORDINATE
			C2825();
			break;

		case C2827: //South of Draynor Village bank, by the fishing spot. MAP
			C2827();
			break;

		case C2829:
			break;

		case C2831: //You'll need to look for a town with a central fountain. CRYPTIC
			C2831();
			break;

		case C2833: //In a town where the guards are armed with maces, search the upstairs rooms of the Public House. CRYPTIC 
			C2833();
			break;

		case C2835: //In a town where thieves steal from stalls, search for some drawers in the upstairs of a house near the bank. CRYPTIC
			C2835();
			break;

		case C2837: //In a town where everyone has perfect vision, seek some locked drawers in a house that sits opposite a workshop. CRYPTIC
			C2837();
			break;

		case C2839: //In a town where wizards are known to gather, search upstairs in a large house to the north. CRYPTIC
			C2839();
			break;

		case C2841: //Speak to Hazelmere. CRYPTIC
			C2841();	
			break;

		case C2843: //OK CO ANAGRAM
			C2843();
			break;

		case C2845: //EEK ZERO OP ANAGRAM
			C2845();
			break;

		case C2847: //EL OW ANAGRAM
			C2847();
			break;

		case C2848: //Speak to Hajedy. CRYPTIC
			C2848();
			break;

		case C2849: //R AK MI ANAGRAM
			C2849();
			break;

		case C2851: //ARE COL ANAGRAM
			C2851();
			break;

		case C2853: //speak to a referee CRYPTIC
			C2853();
			break;

		case C2855: //Speak to Donovan, the Family Handyman. CRYPTIC
			C2855();
			break;

		case C2856: //PEATY PERT ANAGRAM
			C2856();
			break;

		case C2857: //GOBLIN KERN ANAGRAM
			C2857();
			break;

		case C2858: //HALT US ANAGRAM
			C2858();
			break;

		case C3582: //11.03N, 31.20E COORDINATE
			C3582();
			break;

		case C3584: //07.05N, 30.56E COORDINATE
			C3584();
			break;

		case C3586: //11.41N, 14.58E COORDINATE
			C3586();
			break;

		case C3588: //00.13S, 13.58E COORDINATE
			C3588();
			break;

		case C3590: //00.18S, 09.28E COORDINATE
			C3590();
			break;

		case C3592: //08.33N, 01.39W COORDINATE
			C3592();
			break;

		case C3594: //11.05N, 00.45W COORDINATE
			C3594();
			break;

		case C3596: //West of the Crafting Guild. MAP
			C3596();
			break;

		case C3598: //Inside McGrubor's Wood west of Seers' village MAP
			C3598();
			break;

		case C3599: //Just south of East Ardougne (straight south of town square), north of the Tower of Life, near the Necromancer Tower. MAP
			C3599();
			break;

		case C3601: //Search the crate by the Clock Tower, south-west of East Ardougne. MAP
			C3601();
			break;

		case C3602: //Just west of the Chemist's house in Rimmington. MAP
			C3602();
			break;

		case C3604: //In a village made of bamboo, look for some crates under one of the houses. CRYPTIC
			C3604();
			break;

		case C3605: //Search the upstairs drawers of a house in a village where pirates are known to have a good time. CRYPTIC
			C3605();
			break;

		case C3607: //Go to the village being attacked by trolls, search the drawers in one of the houses. CRYPTIC
			C3607();
			break;

		case C3609: //A town with a different sort of night-life is your destination. CRYPTIC
			C3609();
			break;

		case C3610: //Find a crate close to the monks that like to paarty! CRYPTIC
			C3610();
			break;

		case C3611: //ME IF ANAGRAM
			C3611();
			break;

		case C3612: //BAIL TRIMS ANAGRAM
			C3612();
			break;

		case C3613: //A BAS ANAGRAM
			C3613();
			break;

		case C3614: //Speak to Ulizius. CRYPTIC
			C3614();
			break;

		case C3615: //Speak to Roavar. CRYPTIC
			C3615();
			break;

		case C3616: //AHA JAR ANAGRAM
			C3616();
			break;

		case C3617: //Speak to Kangai Mau. CRYPTIC
			C3617();
			break;

		case C3618: //ICY FE ANAGRAM
			C3618();
			break;

		case C7274: //DT RUN B ANAGRAM
			C7274();
			break;

		case C7276: //GOT A BOY ANAGRAM
			C7276();
			break;

		case C7278: //HICK JET ANAGRAM
			C7278();
			break;

		case C7280: //ARC O LINE ANAGRAM
			C7280();
			break;

		case C7282: //NOD MED ANAGRAM
			C7282();
			break;

		case C7284: //LARK IN DOG ANAGRAM
			C7284();
			break;

		case C7286: //On Miscellania, one of the Fremennik Isles, just east of the castle. MAP
			C7286();
			break;

		case C7288: //South of the path to Mort'ton. MAP
			C7288();
			break;

		case C7290: //By the entrance to the Ourania Cave. MAP
			C7290();
			break;

		case C7292: //Road between Rellekka and the Lighthouse. MAP
			C7292();
			break;

		case C7294: //North of Seers' Village, along the path towards Rellekka. MAP
			C7294();
			break;

		case C7296: //The dead, red dragon watches over this chest. He must really dig the view. CRYPTIC
			C7296();
			break;

		case C7298: //Go to this building to be illuminated, and check the drawers while you are there. CRYPTIC
			C7298();
			break;

		case C7300: //Try not to step on any aquatic nasties while searching this crate. CRYPTIC
			C7300();
			break;

		case C7301: //Probably filled with wizards socks. CRYPTIC
			C7301();
			break;

		case C7303: //This crate is mine, all mine, even if it is in the middle of the desert. CRYPTIC
			C7303();
			break;

		case C7304: //This crate holds a better reward than a broken arrow. CRYPTIC
			C7304();
			break;

		case C7305: //22.30N, 03.01E COORDINATE
			C7305();
			break;

		case C7307: //05.20S, 04.28E COORDINATE
			C7307();
			break;

		case C7309: //01.18S, 14.15E COORDINATE
			C7309();
			break;

		case C7311: //09.48N, 17.39E COORDINATE
			C7311();
			break;

		case C7313: //00.20S, 23.15E COORDINATE
			C7313();
			break;

		case C7315: //14.54N, 09.13E COORDINATE
			C7315();
			break;

		case C7317: //03.35S, 13.35E COORDINATE
			C7317();
			break;
			
		case BACKING:
			//BACKING
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Banking";
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
			
			if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth ("))) { //tp to ge directly
				if(Tabs.getOpen() != Tab.EQUIPMENT) {
					Tabs.open(Tab.EQUIPMENT);
					sleep(randomNum(73,212));
				}
				
				Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
				sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
				backing = 0;
				sleep(randomNum(232,440));
			} else if (Inventory.contains("Varrock teleport")) { //BACKUP tp varrock & walk
				if(Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(73,212));
				}
				
				Inventory.interact("Varrock teleport", "Break");
				sleepUntil(() -> varrockcentre.contains(getLocalPlayer()), randomNum(2300, 4500));
				sleep(randomNum(100,400));
				
				while (!grandexchangearea.contains(getLocalPlayer())) {
					if (Walking.shouldWalk(randomNum(5, 8))) {
						Walking.walk(grandexchangeareasmall.getRandomTile());
						sleep(randomNum(200, 400));
					}
				}
				
				if (grandexchangearea.contains(getLocalPlayer())) {
					backing = 0;
				}
			} else if (Inventory.contains(f -> f.getName().contains("Amulet of glory("))) { //BACKUP BACKUP tp edgevulle & walk
				if(Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(73,212));
				}
				int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
				Inventory.slotInteract(glory, "Rub");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2300, 4500));
				sleep(randomNum(50,200));
				Dialogues.chooseOption(1);
				sleepUntil(() -> edgevillecentre.contains(getLocalPlayer()), randomNum(2300, 4500));
				sleep(randomNum(100,400));
				
				while (!grandexchangearea.contains(getLocalPlayer())) {
					if (Walking.shouldWalk(randomNum(5, 8))) {
						Walking.walk(grandexchangeareasmall.getRandomTile());
						sleep(randomNum(200, 400));
					}
				}
				
				if (grandexchangearea.contains(getLocalPlayer())) {
					backing = 0;
				}
			}
			break;
			
		case BANKING:
			
			if (!Inventory.isEmpty()) {
				Bank.depositAllItems();
				sleepUntil(() -> Inventory.isEmpty(), randomNum(1000,2000));
				sleep(randomNum(20, 150));
			}
			
			//FULLHEALTH
			if (getLocalPlayer().getHealthPercent() <= 90 && Bank.contains("Shark")) {
				while (getLocalPlayer().getHealthPercent() != 100) {
					int prevhealth = getLocalPlayer().getHealthPercent();
					Bank.withdraw("Shark");
					sleepUntil(() -> Inventory.contains("Shark"), randomNum(1000,2000));
					sleep(randomNum(40, 150));
					Inventory.interact("Shark", "Eat");
					sleepUntil(() -> getLocalPlayer().getHealthPercent() != prevhealth, randomNum(1000,2000));
					sleep(randomNum(30, 150));
				}
				
			}

			//BUYCHECK
			if (Bank.count("Eclectic impling jar")+Inventory.count("Eclectic impling jar") < 10) {
				lowsupplies[0] = 1;
			} else {
				lowsupplies[0] = 0;
			}
			if (Bank.count("Stamina potion(4)")+Inventory.count("Stamina potion(4)")+Bank.count("Stamina potion(3)")+Inventory.count("Stamina potion(3)") < 2) {
				lowsupplies[1] = 1;
			} else {
				lowsupplies[1] = 0;
			}
			if (Bank.count("Digsite teleport")+Inventory.count("Digsite teleport") < 5) {
				lowsupplies[2] = 1;
			} else {
				lowsupplies[2] = 0;
			}
			if (Bank.count("Games necklace(8)")+Inventory.count("Games necklace(8)")+Bank.count("Games necklace(7)")+Inventory.count("Games necklace(7)")+Bank.count("Games necklace(6)")+Inventory.count("Games necklace(6)")+Bank.count("Games necklace(5)")+Inventory.count("Games necklace(5)")+Bank.count("Games necklace(4)")+Inventory.count("Games necklace(4)") < 2) {
				lowsupplies[3] = 1;
			} else {
				lowsupplies[3] = 0;
			}
			if (Bank.count("Skills necklace(6)")+Inventory.count("Skills necklace(6)")+Bank.count("Skills necklace(5)")+Inventory.count("Skills necklace(5)")+Bank.count("Skills necklace(4)")+Inventory.count("Skills necklace(4)") < 2) {
				lowsupplies[4] = 1;
			} else {
				lowsupplies[4] = 0;
			}
			if (Bank.count("Amulet of glory(6)")+Inventory.count("Amulet of glory(6)")+Bank.count("Amulet of glory(5)")+Inventory.count("Amulet of glory(5)")+Bank.count("Amulet of glory(4)")+Inventory.count("Amulet of glory(4)") < 2) {
				lowsupplies[5] = 1;
			} else {
				lowsupplies[5] = 0;
			}
			if (Bank.count("Teleport to house")+Inventory.count("Teleport to house") < 5) {
				lowsupplies[6] = 1;
			} else {
				lowsupplies[6] = 0;
			}
			if (Bank.count("Varrock teleport")+Inventory.count("Varrock teleport") < 5) {
				lowsupplies[7] = 1;
			} else {
				lowsupplies[7] = 0;
			}
			if (Bank.count("Lumbridge graveyard teleport")+Inventory.count("Lumbridge graveyard teleport") < 5) {
				lowsupplies[8] = 1;
			} else {
				lowsupplies[8] = 0;
			}
			if (Bank.count("Falador teleport")+Inventory.count("Falador teleport") < 5) {
				lowsupplies[9] = 1;
			} else {
				lowsupplies[9] = 0;
			}
			if (Bank.count("Fenkenstrain's castle teleport")+Inventory.count("Fenkenstrain's castle teleport") < 5) {
				lowsupplies[10] = 1;
			} else {
				lowsupplies[10] = 0;
			}
			if (Bank.count("West ardougne teleport")+Inventory.count("West ardougne teleport") < 5) {
				lowsupplies[11] = 1;
			} else {
				lowsupplies[11] = 0;
			}
			if (Bank.count("Ardougne teleport")+Inventory.count("Ardougne teleport") < 5) {
				lowsupplies[12] = 1;
			} else {
				lowsupplies[12] = 0;
			}
			if (Bank.count("Camelot teleport")+Inventory.count("Camelot teleport") < 5) {
				lowsupplies[13] = 1;
			} else {
				lowsupplies[13] = 0;
			}
			if (Bank.count("Draynor manor teleport")+Inventory.count("Draynor manor teleport") < 5) {
				lowsupplies[14] = 1;
			} else {
				lowsupplies[14] = 0;
			}
			if (Bank.count("Shark")+Inventory.count("Shark") < 5) {
				lowsupplies[15] = 1;
			} else {
				lowsupplies[15] = 0;
			}
			if (Equipment.count("Rune arrow")+Inventory.count("Rune arrow")+Bank.count("Rune arrow") <= 100) {
				lowsupplies[16] = 1;
			} else {
				lowsupplies[16] = 0;
			}
			if (Bank.count("Necklace of passage(5)")+Equipment.count("Necklace of passage(5)")+Inventory.count("Necklace of passage(5)") < 3) {
				lowsupplies[17] = 1;
			} else {
				lowsupplies[17] = 0;
			}
			if (Bank.count("Ring of wealth (5)")+Equipment.count("Ring of wealth (5)")+Inventory.count("Ring of wealth (5)") < 3) {
				lowsupplies[18] = 1;
			} else {
				lowsupplies[18] = 0;
			}
			if (Bank.count("Combat bracelet(6)")+Equipment.count("Combat bracelet(6)")+Inventory.count("Combat bracelet(6)") < 3) {
				lowsupplies[19] = 1;
			} else {
				lowsupplies[19] = 0;
			}
			if (Bank.count("Ring of dueling(8)")+Inventory.count("Ring of dueling(8)")+Bank.count("Ring of dueling(7)")+Inventory.count("Ring of dueling(7)")+Bank.count("Ring of dueling(6)")+Inventory.count("Ring of dueling(6)")+Bank.count("Ring of dueling(5)")+Inventory.count("Ring of dueling(5)")+Bank.count("Ring of dueling(4)")+Inventory.count("Ring of dueling(4)") < 3) {
				lowsupplies[20] = 1;
			} else {
				lowsupplies[20] = 0;
			}
			
			//GRANDEXCHANGE BUY & SELL SUPPLIES
			if (Arrays.stream(lowsupplies).anyMatch(i -> i == 1)) {
				Bank.setWithdrawMode(BankMode.NOTE);
				sleep(randomNum(70,180));
				Bank.withdrawAll("Coins");
				sleep(randomNum(70,180));
				
				for(int i = 0; i < sellable.length; i++) {
					if (Bank.contains(sellable[i]) && Inventory.emptySlotCount() != 0) {
						Bank.withdrawAll(sellable[i]);
						sleep(randomNum(30,80));
					}
				}
				
				if (Bank.isOpen()) {
					Bank.close();
					sleepUntil(() -> !Bank.isOpen(), randomNum(950, 1250));
					sleep(randomNum(120,330));
				}
				
				if (!GrandExchange.isOpen() && !Bank.isOpen()) {
					GrandExchange.open();
					sleepUntil(() -> GrandExchange.isOpen(), randomNum(1950, 3250));
					sleep(randomNum(120,330));
				}
				
				if (!Inventory.onlyContains("Coins") && GrandExchange.isOpen()) {
					for(int i = 0; i < sellable.length; i++) {
						if (Inventory.contains(sellable[i])) {
							String itemtosell = sellable[i];
							if (GrandExchange.getFirstOpenSlot() == -1) {
								if (GrandExchange.isReadyToCollect()) {
									int prevmoney = Inventory.count("Coins");
									sleep(randomNum(120,330));
									GrandExchange.collect();
									sleepUntil(() -> prevmoney != Inventory.count("Coins"), randomNum(2000,3000));
									sleep(randomNum(120,300));
								}
								if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
									sleep(randomNum(400,800));
									GrandExchange.cancelAll();
									sleep(randomNum(400,800));
								}
							}
							
							GrandExchange.sellItem(itemtosell, Inventory.count(itemtosell), 1);
							sleepUntil(() -> !Inventory.contains(itemtosell), randomNum(6000,8000));
							sleep(randomNum(420,830));
						}
					}

					if ((GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) || GrandExchange.isReadyToCollect()) {
						if (GrandExchange.isReadyToCollect()) {
							sleep(randomNum(120,330));
							GrandExchange.collect();
							sleep(randomNum(120,300));
						}
						if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
							GrandExchange.cancelAll();
							sleep(randomNum(400,800));
						}
					}
				}
				
				int moneycount = Inventory.count("Coins"); 
				
				if (moneycount <= 100000) {
					log("Not enough money to buy supplies");
					telegramSendMessage("Not enough money to buy supplies, stopping script" + "%0A%0A" + "Bot name: " + dbbotname + ", Rangerboots: "+dbbotrangerboots + ", Run time: " + dbbotruntime + ", Total clues: " + dbbotcluestotal, -485506118);
					stop();
				}

				if (lowsupplies[0] == 1 && !Inventory.isFull()) { //Eclectic impling jar
					GrandExchange.buyItem("Eclectic impling jar", (int) ((moneycount/eclecticimplingjarprice)*0.5), (int) (eclecticimplingjarprice*percentagemarkup));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[1] == 1 && !Inventory.isFull()) { //Stamina potion(4)
					GrandExchange.buyItem("Stamina potion(4)", (int) ((moneycount/stampotprice)*0.07), (int) (stampotprice*percentagemarkup));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[2] == 1 && !Inventory.isFull()) { //Digsite teleport
					GrandExchange.buyItem("Digsite teleport", (int) ((moneycount/digsitetpprice)*0.03), (int) (digsitetpprice*percentagemarkup));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[3] == 1 && !Inventory.isFull()) { //Games necklace(8)
					GrandExchange.buyItem("Games necklace(8)", (int) ((moneycount/gamesnecklaceprice)*0.04), (int) (gamesnecklaceprice*percentagemarkup));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[4] == 1 && !Inventory.isFull()) { //Skills necklace(6)
					GrandExchange.buyItem("Skills necklace(6)", (int) ((moneycount/skillsnecklaceprice)*0.07), (int) (percentagemarkup*skillsnecklaceprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[5] == 1 && !Inventory.isFull()) { //Amulet of glory(6)
					GrandExchange.buyItem("Amulet of glory(6)", (int) ((moneycount/amuletofgloryprice)*0.07), (int) (percentagemarkup*amuletofgloryprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[6] == 1 && !Inventory.isFull()) { //Teleport to house
					GrandExchange.buyItem("Teleport to house", (int) ((moneycount/tptohouseprice)*0.03), (int) (percentagemarkup*tptohouseprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[7] == 1 && !Inventory.isFull()) { //Varrock teleport
					GrandExchange.buyItem("Varrock teleport", (int) ((moneycount/varrockteleportprice)*0.03), (int) (percentagemarkup*varrockteleportprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[8] == 1 && !Inventory.isFull()) { //Lumbridge graveyard teleport
					GrandExchange.buyItem("Lumbridge graveyard teleport", (int) ((moneycount/lumbygraveyardtpprice)*0.05), (int) (percentagemarkup*lumbygraveyardtpprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[9] == 1 && !Inventory.isFull()) { //Falador teleport
					GrandExchange.buyItem("Falador teleport", (int) ((moneycount/faladortpprice)*0.02), (int) (percentagemarkup*faladortpprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[10] == 1 && !Inventory.isFull()) { //Fenkenstrain's castle teleport
					GrandExchange.buyItem("Fenkenstrain's castle teleport", (int) ((moneycount/fenkenstraintpprice)*0.05), (int) (percentagemarkup*fenkenstraintpprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[11] == 1 && !Inventory.isFull()) { //West ardougne teleport
					GrandExchange.buyItem("West ardougne teleport", (int) ((moneycount/westardytpprice)*0.05), (int) (percentagemarkup*westardytpprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[12] == 1 && !Inventory.isFull()) { //Ardougne teleport
					GrandExchange.buyItem("Ardougne teleport", (int) ((moneycount/ardytpprice)*0.02), (int) (percentagemarkup*ardytpprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[13] == 1 && !Inventory.isFull()) { //Camelot teleport
					GrandExchange.buyItem("Camelot teleport", (int) ((moneycount/camelottpprrice)*0.02), (int) (percentagemarkup*camelottpprrice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[14] == 1 && !Inventory.isFull()) { //Draynor manor teleport
					GrandExchange.buyItem("Draynor manor teleport", (int) ((moneycount/draynormanortpprice)*0.05), (int) (percentagemarkup*draynormanortpprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[15] == 1 && !Inventory.isFull()) { //Shark
					GrandExchange.buyItem("Shark", (int) ((moneycount/sharkprice)*0.04), (int) (percentagemarkup*sharkprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[16] == 1 && !Inventory.isFull()) { //Rune arrow
					GrandExchange.buyItem("Rune arrow", (int) ((moneycount/runearrowprice)*0.04), (int) (percentagemarkup*runearrowprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[17] == 1 && !Inventory.isFull()) { //Necklace of passage(5)
					GrandExchange.buyItem("Necklace of passage(5)", (int) ((moneycount/necklaceofpassageprice)*0.07), (int) (percentagemarkup*necklaceofpassageprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[18] == 1 && !Inventory.isFull()) { //Ring of wealth (5)
					GrandExchange.buyItem("Ring of wealth (5)", (int) ((moneycount/ringofwealthprice)*0.1), (int) (percentagemarkup*ringofwealthprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[19] == 1 && !Inventory.isFull()) { //Combat bracelet(6)
					GrandExchange.buyItem("Combat bracelet(6)", (int) ((moneycount/combatbraceletprice)*0.06), (int) (percentagemarkup*combatbraceletprice));
					sleep(randomNum(620,930));
				}
				
				if (GrandExchange.getFirstOpenSlot() == -1 && GrandExchange.isOpen()) {
					if (GrandExchange.isReadyToCollect()) {
						int prevslots = Inventory.emptySlotCount();
						sleep(randomNum(120,330));
						GrandExchange.collect();
						sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
						sleep(randomNum(120,300));
					}
					if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
						sleep(randomNum(400,800));
						GrandExchange.cancelAll();
						sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
						sleep(randomNum(50,300));
					}
				}
				
				if (lowsupplies[20] == 1 && !Inventory.isFull()) { //Ring of dueling(8)
					GrandExchange.buyItem("Ring of dueling(8)", (int) ((moneycount/ringofduelingprice)*0.03), (int) (percentagemarkup*ringofduelingprice));
					sleep(randomNum(620,930));
				}
				
				sleepUntil(() -> GrandExchange.isReadyToCollect(), randomNum(2400,3200));
				
				if (GrandExchange.isReadyToCollect()) {
					int prevslots = Inventory.emptySlotCount();
					sleep(randomNum(120,330));
					GrandExchange.collect();
					sleepUntil(() -> prevslots != Inventory.emptySlotCount(), randomNum(2000,3000));
					sleep(randomNum(120,300));
				}
				if (GrandExchange.slotContainsItem(1) || GrandExchange.slotContainsItem(2) || GrandExchange.slotContainsItem(3) || GrandExchange.slotContainsItem(4) || GrandExchange.slotContainsItem(5) || GrandExchange.slotContainsItem(6) || GrandExchange.slotContainsItem(7) || GrandExchange.slotContainsItem(0)) {
					sleep(randomNum(400,800));
					GrandExchange.cancelAll();
					sleepUntil(() -> !GrandExchange.slotContainsItem(1) && !GrandExchange.slotContainsItem(2) && !GrandExchange.slotContainsItem(3) && !GrandExchange.slotContainsItem(4) && !GrandExchange.slotContainsItem(5) && !GrandExchange.slotContainsItem(6) && !GrandExchange.slotContainsItem(7) && !GrandExchange.slotContainsItem(0), randomNum(2000,3000));
					sleep(randomNum(50,300));
				}
				if (GrandExchange.isOpen()) {
					sleep(randomNum(400,800));
					GrandExchange.close();
					sleepUntil(() -> !GrandExchange.isOpen(), randomNum(2000,3000));
					sleep(randomNum(400,800));
				}
			}
			
			if (!Inventory.contains("Clue scroll (medium)") && !Bank.contains("Clue scroll (medium)") && Bank.count("Eclectic impling jar") >= 10) {
				//WITHDRAW IMPLINGS
				if (Bank.getWithdrawMode() == BankMode.NOTE) {
					Bank.setWithdrawMode(BankMode.ITEM);
					sleep(randomNum(60,230));
				}
				
				Bank.withdraw("Eclectic impling jar", 10);
				sleepUntil(() -> Inventory.contains("Eclectic impling jar"), randomNum(450, 750));
				sleep(randomNum(60,230));
				if (Bank.isOpen()) {
					failsafeopeningimplings = 0;
					Bank.close();
					sleepUntil(() -> !Bank.isOpen(), randomNum(950, 1250));
					sleep(randomNum(60,230));
				}
			} else if ((Inventory.contains("Clue scroll (medium)") || Bank.contains("Clue scroll (medium)")) && Bank.isOpen()) {
				//GEAR
				if (Bank.getWithdrawMode() == BankMode.NOTE) {
					Bank.setWithdrawMode(BankMode.ITEM);
					sleep(randomNum(60,230));
				}
				
				if (!Equipment.contains("Necklace of passage(5)")) {
					if (Inventory.contains("Necklace of passage(5)")) {
						Inventory.interact("Necklace of passage(5)", "Wear");
						sleepUntil(() -> Equipment.contains("Necklace of passage(5)"), randomNum(800,1200));
						sleep(randomNum(60,230));
					} else if (Bank.contains("Necklace of passage(5)")) {
						Bank.withdraw("Necklace of passage(5)", 1);
						sleepUntil(() -> Inventory.contains("Necklace of passage(5)"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Necklace of passage(5)", "Wear");
						sleepUntil(() -> Equipment.contains("Necklace of passage(5)"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Kandarin headgear 1")) {
					if (Inventory.contains("Kandarin headgear 1")) {
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> Equipment.contains("Kandarin headgear 1"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Kandarin headgear 1")) {
						Bank.withdraw("Kandarin headgear 1", 1);
						sleepUntil(() -> Inventory.contains("Kandarin headgear 1"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> Equipment.contains("Kandarin headgear 1"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Green cape")) {
					if (Inventory.contains("Green cape")) {
						Inventory.interact("Green cape", "Wear");
						sleepUntil(() -> Equipment.contains("Green cape"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Green cape")) {
						Bank.withdraw("Green cape", 1);
						sleepUntil(() -> Inventory.contains("Green cape"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Green cape", "Wear");
						sleepUntil(() -> Equipment.contains("Green cape"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Rune arrow") || Equipment.count("Rune arrow") <= 100) {
					if (Inventory.contains("Rune arrow")) {
						Inventory.interact("Rune arrow", "Wield");
						sleepUntil(() -> Equipment.count("Rune arrow") > 60, randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Rune arrow")) {
						Bank.withdrawAll("Rune arrow");
						sleepUntil(() -> Inventory.contains("Rune arrow"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Rune arrow", "Wield");
						sleepUntil(() -> Equipment.count("Rune arrow") > 60, randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Magic shortbow")) {
					if (Inventory.contains("Magic shortbow")) {
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> Equipment.contains("Magic shortbow"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Magic shortbow")) {
						Bank.withdraw("Magic shortbow", 1);
						sleepUntil(() -> Inventory.contains("Magic shortbow"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> Equipment.contains("Magic shortbow"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Green d'hide body")) {
					if (Inventory.contains("Green d'hide body")) {
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide body"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Green d'hide body")) {
						Bank.withdraw("Green d'hide body", 1);
						sleepUntil(() -> Inventory.contains("Green d'hide body"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide body"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Green d'hide chaps")) {
					if (Inventory.contains("Green d'hide chaps")) {
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Green d'hide chaps")) {
						Bank.withdraw("Green d'hide chaps", 1);
						sleepUntil(() -> Inventory.contains("Green d'hide chaps"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Boots of lightness")) {
					if (Inventory.contains("Boots of lightness")) {
						Inventory.interact("Boots of lightness", "Wear");
						sleepUntil(() -> Equipment.contains("Boots of lightness"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Boots of lightness")) {
						Bank.withdraw("Boots of lightness", 1);
						sleepUntil(() -> Inventory.contains("Boots of lightness"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Boots of lightness", "Wear");
						sleepUntil(() -> Equipment.contains("Boots of lightness"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Combat bracelet(6)") && !Equipment.contains("Combat bracelet(5)") && !Equipment.contains("Combat bracelet(4)")) {
					if (Inventory.contains("Combat bracelet(6)")) {
						Inventory.interact("Combat bracelet(6)", "Wear");
						sleepUntil(() -> Equipment.contains("Combat bracelet(6)"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Combat bracelet(6)")) {
						Bank.withdraw("Combat bracelet(6)", 1);
						sleepUntil(() -> Inventory.contains("Combat bracelet(6)"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Combat bracelet(6)", "Wear");
						sleepUntil(() -> Equipment.contains("Combat bracelet(6)"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				if (!Equipment.contains("Ring of wealth (5)") && !Equipment.contains("Ring of wealth (4)")) {
					if (Inventory.contains("Ring of wealth (5)")) {
						Inventory.interact("Ring of wealth (5)", "Wear");
						sleepUntil(() -> Equipment.contains("Ring of wealth (5)"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}else if (Bank.contains("Ring of wealth (5)")) {
						Bank.withdraw("Ring of wealth (5)", 1);
						sleepUntil(() -> Inventory.contains("Ring of wealth (5)"), randomNum(800,1200));
						sleep(randomNum(60,230));
						Inventory.interact("Ring of wealth (5)", "Wear");
						sleepUntil(() -> Equipment.contains("Ring of wealth (5)"), randomNum(800,1200));
						sleep(randomNum(60,230));
					}
					if (!Inventory.isEmpty()) {
						Bank.depositAllItems();
						sleep(randomNum(60,230));
					}
				}
				
				//INVENTORY
				if (!Inventory.contains("Clue scroll (medium)") && Bank.contains("Clue scroll (medium)")) {
					Bank.withdraw("Clue scroll (medium)", 1);
					//sleepUntil(() -> Inventory.contains("Clue scroll (medium)"), randomNum(800,1200));
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Stamina potion(4)") && Bank.contains("Stamina potion(4)")) {
					Bank.withdraw("Stamina potion(4)", 2);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Stamina potion(3)") && Bank.contains("Stamina potion(3)")) {
					Bank.withdraw("Stamina potion(3)", 2);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Spade") && Bank.contains("Spade")) {
					Bank.withdraw("Spade", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Metal key") && Bank.contains("Metal key")) {
					Bank.withdraw("Metal key", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Rope") && Bank.contains("Rope")) {
					Bank.withdraw("Rope", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Digsite teleport") && Bank.contains("Digsite teleport")) {
					Bank.withdrawAll("Digsite teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Ring of dueling(8)") && Bank.contains("Ring of dueling(8)")) {
					Bank.withdraw("Ring of dueling(8)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Ring of dueling(7)") && Bank.contains("Ring of dueling(7)")) {
					Bank.withdraw("Ring of dueling(7)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Ring of dueling(6)") && Bank.contains("Ring of dueling(6)")) {
					Bank.withdraw("Ring of dueling(6)", 1);
					sleep(randomNum(60,230));
				} else if (!Inventory.contains("Ring of dueling(5)") && Bank.contains("Ring of dueling(5)")) {
					Bank.withdraw("Ring of dueling(5)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Ring of dueling(4)") && Bank.contains("Ring of dueling(4)")) {
					Bank.withdraw("Ring of dueling(4)", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Games necklace(8)") && Bank.contains("Games necklace(8)")) {
					Bank.withdraw("Games necklace(8)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Games necklace(7)") && Bank.contains("Games necklace(7)")) {
					Bank.withdraw("Games necklace(7)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Games necklace(6)") && Bank.contains("Games necklace(6)")) {
					Bank.withdraw("Games necklace(6)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Games necklace(5)") && Bank.contains("Games necklace(5)")) {
					Bank.withdraw("Games necklace(5)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Games necklace(4)") && Bank.contains("Games necklace(4)")) {
					Bank.withdraw("Games necklace(4)", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Skills necklace(6)") && Bank.contains("Skills necklace(6)")) {
					Bank.withdraw("Skills necklace(6)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Skills necklace(5)") && Bank.contains("Skills necklace(5)")) {
					Bank.withdraw("Skills necklace(5)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Skills necklace(4)") && Bank.contains("Skills necklace(4)")) {
					Bank.withdraw("Skills necklace(4)", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Amulet of glory(6)") && Bank.contains("Amulet of glory(6)")) {
					Bank.withdraw("Amulet of glory(6)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Amulet of glory(5)") && Bank.contains("Amulet of glory(5)")) {
					Bank.withdraw("Amulet of glory(5)", 1);
					sleep(randomNum(60,230));
				}else if (!Inventory.contains("Amulet of glory(4)") && Bank.contains("Amulet of glory(4)")) {
					Bank.withdraw("Amulet of glory(4)", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Dramen staff") && Bank.contains("Dramen staff")) {
					Bank.withdraw("Dramen staff", 1);
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Teleport to house") && Bank.contains("Teleport to house")) {
					Bank.withdrawAll("Teleport to house");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Varrock teleport") && Bank.contains("Varrock teleport")) {
					Bank.withdrawAll("Varrock teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Lumbridge graveyard teleport") && Bank.contains("Lumbridge graveyard teleport")) {
					Bank.withdrawAll("Lumbridge graveyard teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Falador teleport") && Bank.contains("Falador teleport")) {
					Bank.withdrawAll("Falador teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Fenkenstrain's castle teleport") && Bank.contains("Fenkenstrain's castle teleport")) {
					Bank.withdrawAll("Fenkenstrain's castle teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("West ardougne teleport") && Bank.contains("West ardougne teleport")) {
					Bank.withdrawAll("West ardougne teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Ardougne teleport") && Bank.contains("Ardougne teleport")) {
					Bank.withdrawAll("Ardougne teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Camelot teleport") && Bank.contains("Camelot teleport")) {
					Bank.withdrawAll("Camelot teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Draynor manor teleport") && Bank.contains("Draynor manor teleport")) {
					Bank.withdrawAll("Draynor manor teleport");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Coins") && Bank.contains("Coins")) {
					Bank.withdrawAll("Coins");
					sleep(randomNum(60,230));
				}
				
				if (!Inventory.contains("Shark") && Bank.contains("Shark")) {
					Bank.withdraw("Shark", 1);
					sleepUntil(() -> Inventory.contains("Shark"), randomNum(1000,2000));
					sleep(randomNum(60,230));
				}
				
				if (Inventory.contains("Clue scroll (medium)") && Inventory.contains(f -> f.getName().contains("Stamina potion")) && Inventory.contains("Spade") && Inventory.contains("Metal key") && Inventory.contains("Rope") && Inventory.contains("Digsite teleport") && Inventory.contains(f -> f.getName().contains("Ring of dueling")) && Inventory.contains(f -> f.getName().contains("Games necklace")) && Inventory.contains(f -> f.getName().contains("Skills necklace")) && Inventory.contains(f -> f.getName().contains("Amulet of glory")) && Inventory.contains("Dramen staff") && Inventory.contains("Teleport to house") && Inventory.contains("Varrock teleport") && Inventory.contains("Lumbridge graveyard teleport") && Inventory.contains("Falador teleport") && Inventory.contains("Fenkenstrain's castle teleport") && Inventory.contains("West ardougne teleport") && Inventory.contains("Ardougne teleport") && Inventory.contains("Camelot teleport") && Inventory.contains("Draynor manor teleport") && Inventory.contains("Coins") && Inventory.contains("Shark") && Equipment.contains("Kandarin headgear 1") && Equipment.contains("Green cape") && Equipment.contains(f -> f.getName().contains("Necklace of passage")) && Equipment.contains("Rune arrow") && Equipment.contains("Green d'hide body") && Equipment.contains("Magic shortbow") && Equipment.contains("Green d'hide chaps") && Equipment.contains("Boots of lightness") && Equipment.contains(f -> f.getName().contains("Combat bracelet")) && Equipment.contains(f -> f.getName().contains("Ring of wealth"))) {
					setupcomplete = 1;
					backing = 0;
					Bank.close();
					sleepUntil(() -> !Bank.isOpen(), randomNum(600,1000));
					sleep(randomNum(60,230));
				}
				
			}
			break;
			
		case INTERACTINGBANK:
			if (!grandexchangeareamedium.contains(getLocalPlayer())) {
				if (Walking.shouldWalk(randomNum(2, 3))) {
					Walking.walk(grandexchangeareasmall.getRandomTile());
					sleep(randomNum(200, 400));
				}
			} else if (grandexchangeareamedium.contains(getLocalPlayer())) {
				Bank.open();
				sleepUntil(() -> Bank.isOpen(), randomNum(1000,1500));
				sleep(randomNum(100, 300));
			}
			break;
			
		case OPENINGIMPLINGS:
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			
			int prevcountlocal = Inventory.count("Eclectic impling jar");
			if (Inventory.count("Eclectic impling jar") >= 2) {
				for(int i = 0; i < randomNum(2,3); i++) {
					if (Inventory.contains("Eclectic impling jar")) {
						Inventory.slotInteract(9, "Loot");
						Random r = new Random();
						double mySample = r.nextGaussian()*5+25;
						sleep((int) mySample);
						failsafeopeningimplings++;
					}
				}
			} else if (Inventory.count("Eclectic impling jar") < 2) {
				if (Inventory.contains("Eclectic impling jar")) {
					Inventory.slotInteract(9, "Loot");
					Random r = new Random();
					double mySample = r.nextGaussian()*5+25;
					sleep((int) mySample);
					failsafeopeningimplings++;
				}
			} 
			sleepUntil(() -> prevcountlocal != Inventory.count("Eclectic impling jar"), randomNum(1000,1100));
			sleep(randomNum(25,90));
			if (Inventory.isItemSelected()) {
				Inventory.deselect();
				sleep(randomNum(20,60));
			}
			break;
			
		case OPENINGREWARD:
			if (!Tabs.isOpen(Tab.INVENTORY)) {Tabs.open(Tab.INVENTORY);}
			int slotcount = Inventory.emptySlotCount();
			sleep(randomNum(10, 40));
			Inventory.interact("Reward casket (medium)", "Open");
			sleep(randomNum(415, 522));
			Walking.walk(grandexchangeareasmall.getRandomTile());
			sleepUntil(() -> slotcount != Inventory.emptySlotCount()+1, randomNum(450,600));
			sleep(randomNum(120, 190));
			if (Inventory.contains("Ranger boots")) {
				dbbotrangerboots ++;
				telegramSendMessage("Ranger boots found :)" + "%0A%0A" + "Bot name: " + dbbotname + ", Rangerboots: "+dbbotrangerboots + ", Run time: " + dbbotruntime + ", Total clues: " + dbbotcluestotal, -485506118);
			}
			break;

		}
		return randomNum(310,515);
	}

	//State names
	private enum State{
		BANKING, BACKING, OPENINGREWARD, INTERACTINGBANK, OPENINGIMPLINGS, C2801, C2803, C2805, C2807, C2809, C2811, C2813, C2815, C2817, C2819, C2821, C2823, C2825, C2827, C2829, C2831, C2833, C2835, C2837, C2839, C2841, C2843, C2845, C2847, C2848, C2849, C2851, C2853, C2855, C2856, C2857, C2858, C3582, C3584, C3586, C3588, C3590, C3592, C3594, C3596, C3598, C3599, C3601, C3602, C3604, C3605, C3607, C3609, C3610, C3611, C3612, C3613, C3614, C3615, C3616, C3617, C3618, C7274, C7276, C7278, C7280, C7282, C7284, C7286, C7288, C7290, C7292, C7294, C7296, C7298, C7300, C7301, C7303, C7304, C7305, C7307, C7309, C7311, C7313, C7315, C7317, C10254, C10256, C10258, C10260, C10262, C10264, C10266, C10268, C10270, C10272, C10274, C10276, C10278, C12021, C12023, C12025, C12027, C12029, C12031, C12033, C12035, C12037, C12039, C12041, C12043, C12045, C12047, C12049, C12051, C12053, C12055, C12057, C12059, C12061, C12063, C12065, C12067, C12069, C12071, C19734, C19736, C19738, C19740, C19742, C19744, C19746, C19748, C19750, C19752, C19754, C19756, C19758, C19760, C19762, C19764, C19766, C19768, C19770, C19772, C19774, C19776, C19778, C19780, C23046, C23131, C23133, C23135, C23136, C23137, C23138, C23139, C23140, C23141, C23142, C23143,
	}


	//Checks if a certain condition is met, then return that state.
	private State getState() {

		if (Inventory.contains("Clue scroll (medium)") && setupcomplete == 1) {
			if (Inventory.contains(2801)) {
				state = State.C2801;
			} else if (Inventory.contains(2803)) {
				state = State.C2803;
			} else if (Inventory.contains(2805)) {
				state = State.C2805;
			} else if (Inventory.contains(2807)) {
				state = State.C2807;
			} else if (Inventory.contains(2809)) {
				state = State.C2809;
			} else if (Inventory.contains(2811)) {
				state = State.C2811;
			} else if (Inventory.contains(2813)) {
				state = State.C2813;
			} else if (Inventory.contains(2815)) {
				state = State.C2815;
			} else if (Inventory.contains(2817)) {
				state = State.C2817;
			} else if (Inventory.contains(2819)) {
				state = State.C2819;
			} else if (Inventory.contains(2821)) {
				state = State.C2821;
			} else if (Inventory.contains(2823)) {
				state = State.C2823;
			} else if (Inventory.contains(2825)) {
				state = State.C2825;
			} else if (Inventory.contains(2827)) {
				state = State.C2827;
			} else if (Inventory.contains(2829)) {
				state = State.C2829;
			} else if (Inventory.contains(2831)) {
				state = State.C2831;
			} else if (Inventory.contains(2833)) {
				state = State.C2833;
			} else if (Inventory.contains(2835)) {
				state = State.C2835;
			} else if (Inventory.contains(2837)) {
				state = State.C2837;
			} else if (Inventory.contains(2839)) {
				state = State.C2839;
			} else if (Inventory.contains(2841)) {
				state = State.C2841;
			} else if (Inventory.contains(2843)) {
				state = State.C2843;
			} else if (Inventory.contains(2845)) {
				state = State.C2845;
			} else if (Inventory.contains(2847)) {
				state = State.C2847;
			} else if (Inventory.contains(2848)) {
				state = State.C2848;
			} else if (Inventory.contains(2849)) {
				state = State.C2849;
			} else if (Inventory.contains(2851)) {
				state = State.C2851;
			} else if (Inventory.contains(2853)) {
				state = State.C2853;
			} else if (Inventory.contains(2855)) {
				state = State.C2855;
			} else if (Inventory.contains(2856)) {
				state = State.C2856;
			} else if (Inventory.contains(2857)) {
				state = State.C2857;
			} else if (Inventory.contains(2858)) {
				state = State.C2858;
			} else if (Inventory.contains(3582)) {
				state = State.C3582;
			} else if (Inventory.contains(3584)) {
				state = State.C3584;
			} else if (Inventory.contains(3586)) {
				state = State.C3586;
			} else if (Inventory.contains(3588)) {
				state = State.C3588;
			} else if (Inventory.contains(3590)) {
				state = State.C3590;
			} else if (Inventory.contains(3592)) {
				state = State.C3592;
			} else if (Inventory.contains(3594)) {
				state = State.C3594;
			} else if (Inventory.contains(3596)) {
				state = State.C3596;
			} else if (Inventory.contains(3598)) {
				state = State.C3598;
			} else if (Inventory.contains(3599)) {
				state = State.C3599;
			} else if (Inventory.contains(3601)) {
				state = State.C3601;
			} else if (Inventory.contains(3602)) {
				state = State.C3602;
			} else if (Inventory.contains(3604)) {
				state = State.C3604;
			} else if (Inventory.contains(3605)) {
				state = State.C3605;
			} else if (Inventory.contains(3607)) {
				state = State.C3607;
			} else if (Inventory.contains(3609)) {
				state = State.C3609;
			} else if (Inventory.contains(3610)) {
				state = State.C3610;
			} else if (Inventory.contains(3611)) {
				state = State.C3611;
			} else if (Inventory.contains(3612)) {
				state = State.C3612;
			} else if (Inventory.contains(3613)) {
				state = State.C3613;
			} else if (Inventory.contains(3614)) {
				state = State.C3614;
			} else if (Inventory.contains(3615)) {
				state = State.C3615;
			} else if (Inventory.contains(3616)) {
				state = State.C3616;
			} else if (Inventory.contains(3617)) {
				state = State.C3617;
			} else if (Inventory.contains(3618)) {
				state = State.C3618;
			} else if (Inventory.contains(7274)) {
				state = State.C7274;
			} else if (Inventory.contains(7276)) {
				state = State.C7276;
			} else if (Inventory.contains(7278)) {
				state = State.C7278;
			} else if (Inventory.contains(7280)) {
				state = State.C7280;
			} else if (Inventory.contains(7282)) {
				state = State.C7282;
			} else if (Inventory.contains(7284)) {
				state = State.C7284;
			} else if (Inventory.contains(7286)) {
				state = State.C7286;
			} else if (Inventory.contains(7288)) {
				state = State.C7288;
			} else if (Inventory.contains(7290)) {
				state = State.C7290;
			} else if (Inventory.contains(7292)) {
				state = State.C7292;
			} else if (Inventory.contains(7294)) {
				state = State.C7294;
			} else if (Inventory.contains(7296)) {
				state = State.C7296;
			} else if (Inventory.contains(7298)) {
				state = State.C7298;
			} else if (Inventory.contains(7300)) {
				state = State.C7300;
			} else if (Inventory.contains(7301)) {
				state = State.C7301;
			} else if (Inventory.contains(7303)) {
				state = State.C7303;
			} else if (Inventory.contains(7304)) {
				state = State.C7304;
			} else if (Inventory.contains(7305)) {
				state = State.C7305;
			} else if (Inventory.contains(7307)) {
				state = State.C7307;
			} else if (Inventory.contains(7309)) {
				state = State.C7309;
			} else if (Inventory.contains(7311)) {
				state = State.C7311;
			} else if (Inventory.contains(7313)) {
				state = State.C7313;
			} else if (Inventory.contains(7315)) {
				state = State.C7315;
			} else if (Inventory.contains(7317)) {
				state = State.C7317;
			} else if (Inventory.contains(10254)) {
				state = State.C10254;
			} else if (Inventory.contains(10256)) {
				state = State.C10256;
			} else if (Inventory.contains(10258)) {
				state = State.C10258;
			} else if (Inventory.contains(10260)) {
				state = State.C10260;
			} else if (Inventory.contains(10262)) {
				state = State.C10262;
			} else if (Inventory.contains(10264)) {
				state = State.C10264;
			} else if (Inventory.contains(10266)) {
				state = State.C10266;
			} else if (Inventory.contains(10268)) {
				state = State.C10268;
			} else if (Inventory.contains(10270)) {
				state = State.C10270;
			} else if (Inventory.contains(10272)) {
				state = State.C10272;
			} else if (Inventory.contains(10274)) {
				state = State.C10274;
			} else if (Inventory.contains(10276)) {
				state = State.C10276;
			} else if (Inventory.contains(10278)) {
				state = State.C10278;
			} else if (Inventory.contains(12021)) {
				state = State.C12021;
			} else if (Inventory.contains(12023)) {
				state = State.C12023;
			} else if (Inventory.contains(12025)) {
				state = State.C12025;
			} else if (Inventory.contains(12027)) {
				state = State.C12027;
			} else if (Inventory.contains(12029)) {
				state = State.C12029;
			} else if (Inventory.contains(12031)) {
				state = State.C12031;
			} else if (Inventory.contains(12033)) {
				state = State.C12033;
			} else if (Inventory.contains(12035)) {
				state = State.C12035;
			} else if (Inventory.contains(12037)) {
				state = State.C12037;
			} else if (Inventory.contains(12039)) {
				state = State.C12039;
			} else if (Inventory.contains(12041)) {
				state = State.C12041;
			} else if (Inventory.contains(12043)) {
				state = State.C12043;
			} else if (Inventory.contains(12045)) {
				state = State.C12045;
			} else if (Inventory.contains(12047)) {
				state = State.C12047;
			} else if (Inventory.contains(12049)) {
				state = State.C12049;
			} else if (Inventory.contains(12051)) {
				state = State.C12051;
			} else if (Inventory.contains(12053)) {
				state = State.C12053;
			} else if (Inventory.contains(12055)) {
				state = State.C12055;
			} else if (Inventory.contains(12057)) {
				state = State.C12057;
			} else if (Inventory.contains(12059)) {
				state = State.C12059;
			} else if (Inventory.contains(12061)) {
				state = State.C12061;
			} else if (Inventory.contains(12063)) {
				state = State.C12063;
			} else if (Inventory.contains(12065)) {
				state = State.C12065;
			} else if (Inventory.contains(12067)) {
				state = State.C12067;
			} else if (Inventory.contains(12069)) {
				state = State.C12069;
			} else if (Inventory.contains(12071)) {
				state = State.C12071;
			} else if (Inventory.contains(19734)) {
				state = State.C19734;
			} else if (Inventory.contains(19736)) {
				state = State.C19736;
			} else if (Inventory.contains(19738)) {
				state = State.C19738;
			} else if (Inventory.contains(19740)) {
				state = State.C19740;
			} else if (Inventory.contains(19742)) {
				state = State.C19742;
			} else if (Inventory.contains(19744)) {
				state = State.C19744;
			} else if (Inventory.contains(19746)) {
				state = State.C19746;
			} else if (Inventory.contains(19748)) {
				state = State.C19748;
			} else if (Inventory.contains(19750)) {
				state = State.C19750;
			} else if (Inventory.contains(19752)) {
				state = State.C19752;
			} else if (Inventory.contains(19754)) {
				state = State.C19754;
			} else if (Inventory.contains(19756)) {
				state = State.C19756;
			} else if (Inventory.contains(19758)) {
				state = State.C19758;
			} else if (Inventory.contains(19760)) {
				state = State.C19760;
			} else if (Inventory.contains(19762)) {
				state = State.C19762;
			} else if (Inventory.contains(19764)) {
				state = State.C19764;
			} else if (Inventory.contains(19766)) {
				state = State.C19766;
			} else if (Inventory.contains(19768)) {
				state = State.C19768;
			} else if (Inventory.contains(19770)) {
				state = State.C19770;
			} else if (Inventory.contains(19772)) {
				state = State.C19772;
			} else if (Inventory.contains(19774)) {
				state = State.C19774;
			} else if (Inventory.contains(19776)) {
				state = State.C19776;
			} else if (Inventory.contains(19778)) {
				state = State.C19778;
			} else if (Inventory.contains(19780)) {
				state = State.C19780;
			} else if (Inventory.contains(23046)) {
				state = State.C23046;
			} else if (Inventory.contains(23131)) {
				state = State.C23131;
			} else if (Inventory.contains(23133)) {
				state = State.C23133;
			} else if (Inventory.contains(23135)) {
				state = State.C23135;
			} else if (Inventory.contains(23136)) {
				state = State.C23136;
			} else if (Inventory.contains(23137)) {
				state = State.C23137;
			} else if (Inventory.contains(23138)) {
				state = State.C23138;
			} else if (Inventory.contains(23139)) {
				state = State.C23139;
			} else if (Inventory.contains(23140)) {
				state = State.C23140;
			} else if (Inventory.contains(23141)) {
				state = State.C23141;
			} else if (Inventory.contains(23142)) {
				state = State.C23142;
			} else if (Inventory.contains(23143)) {
				state = State.C23143;
			}
		} else if (setupcomplete == 0) {
			if (Bank.isOpen()) {
				state = State.BANKING;
			} else if ((Inventory.contains("Reward casket (medium)") && !grandexchangearea.contains(getLocalPlayer()) && !Bank.isOpen()) || backing == 1) {
				state = State.BACKING;
			} else if (Inventory.contains("Reward casket (medium)") && grandexchangearea.contains(getLocalPlayer()) && !Bank.isOpen()) {
				state = State.OPENINGREWARD;
			} else if ((!Inventory.contains(f -> f.getName().contentEquals("Eclectic impling jar") && !f.isNoted()) || Inventory.contains("Clue scroll (medium)")) && grandexchangearea.contains(getLocalPlayer()) && !Bank.isOpen()) {
				state = State.INTERACTINGBANK;
			} else if (Inventory.contains(f -> f.getName().contentEquals("Eclectic impling jar") && !f.isNoted()) && !Inventory.contains("Clue scroll (medium)") && grandexchangearea.contains(getLocalPlayer()) && !Bank.isOpen() && failsafeopeningimplings <= 20) {
				state = State.OPENINGIMPLINGS;
			}
		} else if (Inventory.contains("Reward casket (medium)") && setupcomplete == 1) {
			setupcomplete = 0;
			sleep(randomNum(110,200));
		}

		return state;
	}

	//When script start load this.
	public void onStart() {
		log("Bot Started");
		
		timeBegan = System.currentTimeMillis();
		dbbotname = getLocalPlayer().getName();
		dbbotworld = Client.getCurrentWorld();
		dbbottask = "Fetching GE prices";
		dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
		dbbotrangerboots = 0;
		dbbotcluestotal = 0;
		dbbotcluesperhour = 0;
		dbbotonline = 1;
		onlineBotInsert(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		
		Mouse.getMouseSettings();
		MouseSettings.setSpeed(randomNum(80,90));
		
		//Fetching prices from rsbuddy API
		eclecticimplingjarprice = FetchGE.getBuyAverage_rsbuddy(11248);
		//log("eclecticimplingjarprice: "+eclecticimplingjarprice);
		stampotprice = FetchGE.getBuyAverage_rsbuddy(12625);
		//log("stampotprice: "+stampotprice);
		digsitetpprice = FetchGE.getBuyAverage_rsbuddy(12403);
		//log("digsitetpprice: "+digsitetpprice);
		gamesnecklaceprice = FetchGE.getBuyAverage_rsbuddy(3853);
		//log("gamesnecklaceprice: "+gamesnecklaceprice);
		skillsnecklaceprice = FetchGE.getBuyAverage_rsbuddy(11105);
		//log("skillsnecklaceprice: "+skillsnecklaceprice);
		amuletofgloryprice = FetchGE.getBuyAverage_rsbuddy(11978);
		//log("amuletofgloryprice: "+amuletofgloryprice);
		tptohouseprice = FetchGE.getBuyAverage_rsbuddy(8013);
		//log("tptohouseprice: "+tptohouseprice);
		varrockteleportprice = FetchGE.getBuyAverage_rsbuddy(8007);
		//log("varrockteleportprice: "+varrockteleportprice);
		lumbygraveyardtpprice = FetchGE.getBuyAverage_rsbuddy(19613);
		//log("lumbygraveyardtpprice: "+lumbygraveyardtpprice);
		faladortpprice = FetchGE.getBuyAverage_rsbuddy(8009);
		//log("faladortpprice: "+faladortpprice);
		fenkenstraintpprice = FetchGE.getBuyAverage_rsbuddy(19621);
		//log("fenkenstraintpprice: "+fenkenstraintpprice);
		westardytpprice = FetchGE.getBuyAverage_rsbuddy(19623);
		//log("westardytpprice: "+westardytpprice);
		ardytpprice = FetchGE.getBuyAverage_rsbuddy(8011);
		//log("ardytpprice: "+ardytpprice);
		camelottpprrice = FetchGE.getBuyAverage_rsbuddy(8010);
		//log("camelottpprrice: "+camelottpprrice);
		draynormanortpprice = FetchGE.getBuyAverage_rsbuddy(19615);
		//log("draynormanortpprice: "+draynormanortpprice);
		sharkprice = FetchGE.getBuyAverage_rsbuddy(385);
		//log("sharkprice: "+sharkprice);
		runearrowprice = FetchGE.getBuyAverage_rsbuddy(892);
		//log("runearrowprice: "+runearrowprice);
		necklaceofpassageprice = FetchGE.getBuyAverage_rsbuddy(21146);
		//log("necklaceofpassageprice: "+necklaceofpassageprice);
		ringofwealthprice = FetchGE.getBuyAverage_rsbuddy(11980);
		//log("ringofwealthprice: "+ringofwealthprice);
		combatbraceletprice = FetchGE.getBuyAverage_rsbuddy(11972);
		//log("combatbraceletprice: "+combatbraceletprice);
		ringofduelingprice = FetchGE.getBuyAverage_rsbuddy(2552);
		//log("ringofduelingprice: "+ringofduelingprice);
		
		if (eclecticimplingjarprice == 0) {
			eclecticimplingjarprice = FetchGE.getCurrentPrice_official(11248);
		}
		if (stampotprice == 0) {
			stampotprice = FetchGE.getCurrentPrice_official(12625);
		}
		if (digsitetpprice == 0) {
			digsitetpprice = FetchGE.getCurrentPrice_official(12403);
		}
		if (gamesnecklaceprice == 0) {
			gamesnecklaceprice = FetchGE.getCurrentPrice_official(3853);
		}
		if (skillsnecklaceprice == 0) {
			skillsnecklaceprice = FetchGE.getCurrentPrice_official(11105);
		}
		if (amuletofgloryprice == 0) {
			amuletofgloryprice = FetchGE.getCurrentPrice_official(11978);
		}
		if (tptohouseprice == 0) {
			tptohouseprice = FetchGE.getCurrentPrice_official(8013);
		}
		if (varrockteleportprice == 0) {
			varrockteleportprice = FetchGE.getCurrentPrice_official(8007);
		}
		if (lumbygraveyardtpprice == 0) {
			lumbygraveyardtpprice = FetchGE.getCurrentPrice_official(19613);
		}
		if (faladortpprice == 0) {
			faladortpprice = FetchGE.getCurrentPrice_official(8009);
		}
		if (fenkenstraintpprice == 0) {
			fenkenstraintpprice = FetchGE.getCurrentPrice_official(19621);
		}
		if (westardytpprice == 0) {
			westardytpprice = FetchGE.getCurrentPrice_official(19623);
		}
		if (ardytpprice == 0) {
			ardytpprice = FetchGE.getCurrentPrice_official(8011);
		}
		if (camelottpprrice == 0) {
			camelottpprrice = FetchGE.getCurrentPrice_official(8010);
		}
		if (draynormanortpprice == 0) {
			draynormanortpprice = FetchGE.getCurrentPrice_official(19615);
		}
		if (sharkprice == 0) {
			sharkprice = FetchGE.getCurrentPrice_official(385);
		}
		if (runearrowprice == 0) {
			runearrowprice = FetchGE.getCurrentPrice_official(892);
		}
		if (necklaceofpassageprice == 0) {
			necklaceofpassageprice = FetchGE.getCurrentPrice_official(21146);
		}
		if (ringofwealthprice == 0) {
			ringofwealthprice = FetchGE.getCurrentPrice_official(11980);
		}
		if (combatbraceletprice == 0) {
			combatbraceletprice = FetchGE.getCurrentPrice_official(11972);
		}
		if (ringofduelingprice == 0) {
			ringofduelingprice = FetchGE.getCurrentPrice_official(2552);
		}
		
		if (eclecticimplingjarprice == 0) {
			eclecticimplingjarprice = 2630;
		}
		if (stampotprice == 0) {
			stampotprice = 4846;
		}
		if (digsitetpprice == 0) {
			digsitetpprice = 10550;
		}
		if (gamesnecklaceprice == 0) {
			gamesnecklaceprice = 749;
		}
		if (skillsnecklaceprice == 0) {
			skillsnecklaceprice = 12509;
		}
		if (amuletofgloryprice == 0) {
			amuletofgloryprice = 12850;
		}
		if (tptohouseprice == 0) {
			tptohouseprice = 515;
		}
		if (varrockteleportprice == 0) {
			varrockteleportprice = 544;
		}
		if (lumbygraveyardtpprice == 0) {
			lumbygraveyardtpprice = 1010;
		}
		if (faladortpprice == 0) {
			faladortpprice = 457;
		}
		if (fenkenstraintpprice == 0) {
			fenkenstraintpprice = 1845;
		}
		if (westardytpprice == 0) {
			westardytpprice = 2198;
		}
		if (ardytpprice == 0) {
			ardytpprice = 499;
		}
		if (camelottpprrice == 0) {
			camelottpprrice = 390;
		}
		if (draynormanortpprice == 0) {
			draynormanortpprice = 2300;
		}
		if (sharkprice == 0) {
			sharkprice = 782;
		}
		if (runearrowprice == 0) {
			runearrowprice = 55;
		}
		if (necklaceofpassageprice == 0) {
			necklaceofpassageprice = 808;
		}
		if (ringofwealthprice == 0) {
			ringofwealthprice = 12790;
		}
		if (combatbraceletprice == 0) {
			combatbraceletprice = 12497;
		}
		if (ringofduelingprice == 0) {
			ringofduelingprice = 880;
		}
		
		dbbotname = getLocalPlayer().getName();
		dbbotworld = Client.getCurrentWorld();
		dbbottask = "Loading webnodes";
		dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
		onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		
		while (!WebFinder.isLoaded()) {
            sleep(200); //Allow web nodes to load in propperly
        }

		AbstractWebNode webNode10 = new BasicWebNode(1605, 3526);
		AbstractWebNode webNode11 = new BasicWebNode(1599, 3529);
		AbstractWebNode webNode12 = new BasicWebNode(1597, 3535);
		AbstractWebNode webNode13 = new BasicWebNode(1589, 3538);
		AbstractWebNode webNode14 = new BasicWebNode(1583, 3538);
		AbstractWebNode webNode7 = new BasicWebNode(1626, 3526);
		AbstractWebNode webNode8 = new BasicWebNode(1619, 3525);
		AbstractWebNode webNode9 = new BasicWebNode(1612, 3526);
		AbstractWebNode webNode990 = new BasicWebNode(2392, 3427);
		AbstractWebNode webNode991 = new BasicWebNode(2383, 3428);
		AbstractWebNode webNode992 = new BasicWebNode(2383, 3435);
		AbstractWebNode webNode993 = new BasicWebNode(2382, 3441);
		AbstractWebNode webNode994 = new BasicWebNode(2377, 3447);
		AbstractWebNode webNode995 = new BasicWebNode(2377, 3455);
		AbstractWebNode webNode996 = new BasicWebNode(2381, 3460);
		AbstractWebNode webNode997 = new BasicWebNode(2382, 3467);
		AbstractWebNode webNode330 = new BasicWebNode(2855, 9569);
		AbstractWebNode webNode331 = new BasicWebNode(2862, 9572);
		AbstractWebNode webNode3310 = new BasicWebNode(2837, 9605);
		AbstractWebNode webNode3311 = new BasicWebNode(2841, 9611);
		AbstractWebNode webNode3312 = new BasicWebNode(2838, 9621);
		AbstractWebNode webNode3313 = new BasicWebNode(2842, 9630);
		AbstractWebNode webNode3314 = new BasicWebNode(2842, 9640);
		AbstractWebNode webNode3315 = new BasicWebNode(2838, 9649);
		AbstractWebNode webNode3316 = new BasicWebNode(2834, 9656);
		AbstractWebNode webNode332 = new BasicWebNode(2851, 9577);
		AbstractWebNode webNode333 = new BasicWebNode(2841, 9583);
		AbstractWebNode webNode334 = new BasicWebNode(2836, 9576);
		AbstractWebNode webNode335 = new BasicWebNode(2837, 9565);
		AbstractWebNode webNode336 = new BasicWebNode(2837, 9557);
		AbstractWebNode webNode337 = new BasicWebNode(2845, 9557);
		AbstractWebNode webNode338 = new BasicWebNode(2834, 9587);
		AbstractWebNode webNode339 = new BasicWebNode(2836, 9597);
		AbstractWebNode webNode660 = new BasicWebNode(2834, 3258);
		AbstractWebNode webNode661 = new BasicWebNode(2833, 3267);
		AbstractWebNode webNode6610 = new BasicWebNode(2838, 3236);
		AbstractWebNode webNode6611 = new BasicWebNode(2830, 3235);
		AbstractWebNode webNode662 = new BasicWebNode(2831, 3277);
		AbstractWebNode webNode663 = new BasicWebNode(2823, 3273);
		AbstractWebNode webNode664 = new BasicWebNode(2823, 3264);
		AbstractWebNode webNode665 = new BasicWebNode(2825, 3254);
		AbstractWebNode webNode666 = new BasicWebNode(2830, 3246);
		AbstractWebNode webNode667 = new BasicWebNode(2840, 3247);
		AbstractWebNode webNode668 = new BasicWebNode(2849, 3247);
		AbstractWebNode webNode669 = new BasicWebNode(2847, 3241);
		AbstractWebNode webNode880 = new BasicWebNode(3575, 9926);
		AbstractWebNode webNode881 = new BasicWebNode(3569, 9935);
		AbstractWebNode webNode8810 = new BasicWebNode(3512, 9964);
		AbstractWebNode webNode8811 = new BasicWebNode(3505, 9969);
		AbstractWebNode webNode882 = new BasicWebNode(3561, 9943);
		AbstractWebNode webNode883 = new BasicWebNode(3552, 9949);
		AbstractWebNode webNode884 = new BasicWebNode(3545, 9958);
		AbstractWebNode webNode885 = new BasicWebNode(3539, 9962);
		AbstractWebNode webNode886 = new BasicWebNode(3531, 9968);
		AbstractWebNode webNode887 = new BasicWebNode(3524, 9962);
		AbstractWebNode webNode888 = new BasicWebNode(3517, 9957);
		AbstractWebNode webNode889 = new BasicWebNode(3508, 9958);
		AbstractWebNode webNode760 = new BasicWebNode(2741, 3549);
		AbstractWebNode webNode761 = new BasicWebNode(2742, 3560);
		AbstractWebNode webNode762 = new BasicWebNode(2742, 3568);
		AbstractWebNode webNode763 = new BasicWebNode(2739, 3574);
		AbstractWebNode webNode764 = new BasicWebNode(2736, 3580);
		AbstractWebNode webNode02200 = new BasicWebNode(3423, 3016);
		AbstractWebNode webNode02201 = new BasicWebNode(3430, 3022);
		AbstractWebNode webNode022010 = new BasicWebNode(3491, 3072);
		AbstractWebNode webNode022011 = new BasicWebNode(3502, 3073);
		AbstractWebNode webNode022012 = new BasicWebNode(3510, 3074);
		AbstractWebNode webNode02202 = new BasicWebNode(3438, 3027);
		AbstractWebNode webNode02203 = new BasicWebNode(3443, 3035);
		AbstractWebNode webNode02204 = new BasicWebNode(3447, 3044);
		AbstractWebNode webNode02205 = new BasicWebNode(3449, 3055);
		AbstractWebNode webNode02206 = new BasicWebNode(3451, 3067);
		AbstractWebNode webNode02207 = new BasicWebNode(3457, 3074);
		AbstractWebNode webNode02208 = new BasicWebNode(3467, 3072);
		AbstractWebNode webNode02209 = new BasicWebNode(3478, 3073);
		AbstractWebNode webNode1220 = new BasicWebNode(2782, 3273);
		AbstractWebNode webNode1221 = new BasicWebNode(2775, 3273);
		AbstractWebNode webNode1222 = new BasicWebNode(2768, 3276);
		AbstractWebNode webNode1223 = new BasicWebNode(2764, 3274);
		AbstractWebNode webNode9970 = new BasicWebNode(2825, 3284);
		AbstractWebNode webNode9971 = new BasicWebNode(2829, 3292);
		AbstractWebNode webNode9972 = new BasicWebNode(2838, 3293);
		AbstractWebNode webNode9973 = new BasicWebNode(2848, 3297);
		AbstractWebNode webNode88750 = new BasicWebNode(3549, 3541);
		AbstractWebNode webNode88751 = new BasicWebNode(3547, 3548);
		AbstractWebNode webNode88752 = new BasicWebNode(3549, 3555);
		AbstractWebNode webNode88753 = new BasicWebNode(3549, 3560);
		AbstractWebNode webNode5660 = new BasicWebNode(2909, 3540);
		AbstractWebNode webNode5661 = new BasicWebNode(2916, 3537);
		AbstractWebNode webNode5662 = new BasicWebNode(2920, 3534);
		AbstractWebNode webNode66550 = new BasicWebNode(2921, 3572);
		AbstractWebNode webNode66551 = new BasicWebNode(2922, 3576);
		AbstractWebNode webNode524170 = new BasicWebNode(2525, 3496);
		AbstractWebNode webNode524171 = new BasicWebNode(2516, 3495);
		AbstractWebNode webNode524172 = new BasicWebNode(2510, 3493);
		AbstractWebNode webNode823920 = new BasicWebNode(2606, 3213);
		AbstractWebNode webNode823921 = new BasicWebNode(2612, 3209);
		AbstractWebNode webNode823922 = new BasicWebNode(2616, 3204);

		if (Walking.getWebPathFinder().getNearest(new Tile(1635, 3531), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(1635, 3531), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(1641, 3533), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(1641, 3533), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(1630, 3540), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(1630, 3540), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(1655, 3504), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(1655, 3504), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(1646, 3505), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(1646, 3505), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(1646, 3496), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(1646, 3496), 2).getIndex());
		}
		
		if (Walking.getWebPathFinder().getNearest(new Tile(2352, 3519), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2352, 3519), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2360, 3513), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2360, 3513), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2365, 3505), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2365, 3505), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2365, 3496), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2365, 3496), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2365, 3487), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2365, 3487), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2367, 3478), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2367, 3478), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2368, 3469), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2368, 3469), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2370, 3458), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2370, 3458), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2366, 3449), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2366, 3449), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2365, 3441), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2365, 3441), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2366, 3431), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2366, 3431), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2367, 3422), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2367, 3422), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2367, 3414), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2367, 3414), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2370, 3404), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2370, 3404), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2374, 3396), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2374, 3396), 2).getIndex());
		}
		if (Walking.getWebPathFinder().getNearest(new Tile(2381, 3389), 2) != null) {
			Walking.getWebPathFinder().removeNode(Walking.getWebPathFinder().getNearest(new Tile(2381, 3389), 2).getIndex());
		}
		
		webNode823920.addConnections(Walking.getWebPathFinder().getNearest(new Tile(2606, 3219), 2));
		Walking.getWebPathFinder().getNearest(new Tile(2606, 3219), 2).addConnections(webNode823920);
		webNode823920.addConnections(webNode823921);
		webNode823921.addConnections(webNode823920);
		webNode823921.addConnections(webNode823922);
		webNode823922.addConnections(webNode823921);
		
		webNode7.addConnections(Walking.getWebPathFinder().getNearest(new Tile(1635, 3523), 2));
		Walking.getWebPathFinder().getNearest(new Tile(1635, 3523), 2).addConnections(webNode7);
		webNode14.addConnections(Walking.getWebPathFinder().getNearest(new Tile(1580, 3531), 2));
		Walking.getWebPathFinder().getNearest(new Tile(1580, 3531), 2).addConnections(webNode14);

		webNode10.addConnections(webNode11);
		webNode10.addConnections(webNode9);
		webNode11.addConnections(webNode10);
		webNode11.addConnections(webNode12);
		webNode12.addConnections(webNode11);
		webNode12.addConnections(webNode13);
		webNode13.addConnections(webNode12);
		webNode13.addConnections(webNode14);
		webNode14.addConnections(webNode13);

		webNode7.addConnections(webNode8);
		webNode8.addConnections(webNode7);
		webNode8.addConnections(webNode9);
		webNode9.addConnections(webNode10);
		webNode9.addConnections(webNode8);
		
		webNode990.addConnections(Walking.getWebPathFinder().getNearest(new Tile(2398, 3424), 2));
		Walking.getWebPathFinder().getNearest(new Tile(2398, 3424), 2).addConnections(webNode990);
		
		webNode990.addConnections(webNode991);
		webNode991.addConnections(webNode990);
		webNode991.addConnections(webNode992);
		webNode992.addConnections(webNode991);
		webNode992.addConnections(webNode993);
		webNode993.addConnections(webNode992);
		webNode993.addConnections(webNode994);
		webNode994.addConnections(webNode993);
		webNode994.addConnections(webNode995);
		webNode995.addConnections(webNode994);
		webNode995.addConnections(webNode996);
		webNode996.addConnections(webNode995);
		webNode996.addConnections(webNode997);
		webNode997.addConnections(webNode996);
		
		webNode330.addConnections(webNode331);
		webNode330.addConnections(webNode332);
		webNode331.addConnections(webNode330);
		webNode3310.addConnections(webNode3311);
		webNode3310.addConnections(webNode339);
		webNode3311.addConnections(webNode3310);
		webNode3311.addConnections(webNode3312);
		webNode3312.addConnections(webNode3311);
		webNode3312.addConnections(webNode3313);
		webNode3313.addConnections(webNode3312);
		webNode3313.addConnections(webNode3314);
		webNode3314.addConnections(webNode3313);
		webNode3314.addConnections(webNode3315);
		webNode3315.addConnections(webNode3314);
		webNode3315.addConnections(webNode3316);
		webNode3316.addConnections(webNode3315);
		webNode332.addConnections(webNode330);
		webNode332.addConnections(webNode333);
		webNode333.addConnections(webNode332);
		webNode333.addConnections(webNode334);
		webNode333.addConnections(webNode338);
		webNode334.addConnections(webNode333);
		webNode334.addConnections(webNode335);
		webNode335.addConnections(webNode334);
		webNode335.addConnections(webNode336);
		webNode336.addConnections(webNode335);
		webNode336.addConnections(webNode337);
		webNode337.addConnections(webNode336);
		webNode338.addConnections(webNode333);
		webNode338.addConnections(webNode339);
		webNode339.addConnections(webNode3310);
		webNode339.addConnections(webNode338);
		
		webNode660.addConnections(webNode661);
		webNode661.addConnections(webNode660);
		webNode661.addConnections(webNode662);
		webNode6610.addConnections(webNode6611);
		webNode6610.addConnections(webNode669);
		webNode6611.addConnections(webNode6610);
		webNode662.addConnections(webNode661);
		webNode662.addConnections(webNode663);
		webNode663.addConnections(webNode662);
		webNode663.addConnections(webNode664);
		webNode664.addConnections(webNode663);
		webNode664.addConnections(webNode665);
		webNode665.addConnections(webNode664);
		webNode665.addConnections(webNode666);
		webNode666.addConnections(webNode665);
		webNode666.addConnections(webNode667);
		webNode667.addConnections(webNode666);
		webNode667.addConnections(webNode668);
		webNode668.addConnections(webNode667);
		webNode668.addConnections(webNode669);
		webNode669.addConnections(webNode6610);
		webNode669.addConnections(webNode668);
		
		webNode880.addConnections(webNode881);
		webNode881.addConnections(webNode880);
		webNode881.addConnections(webNode882);
		webNode8810.addConnections(webNode8811);
		webNode8810.addConnections(webNode889);
		webNode8811.addConnections(webNode8810);
		webNode882.addConnections(webNode881);
		webNode882.addConnections(webNode883);
		webNode883.addConnections(webNode882);
		webNode883.addConnections(webNode884);
		webNode884.addConnections(webNode883);
		webNode884.addConnections(webNode885);
		webNode885.addConnections(webNode884);
		webNode885.addConnections(webNode886);
		webNode886.addConnections(webNode885);
		webNode886.addConnections(webNode887);
		webNode887.addConnections(webNode886);
		webNode887.addConnections(webNode888);
		webNode888.addConnections(webNode887);
		webNode888.addConnections(webNode889);
		webNode889.addConnections(webNode8810);
		webNode889.addConnections(webNode888);
		
		webNode760.addConnections(Walking.getWebPathFinder().getNearest(new Tile(2741, 3537), 2));
		Walking.getWebPathFinder().getNearest(new Tile(2741, 3537), 2).addConnections(webNode760);
		
		webNode760.addConnections(webNode761);
		webNode761.addConnections(webNode760);
		webNode761.addConnections(webNode762);
		webNode762.addConnections(webNode761);
		webNode762.addConnections(webNode763);
		webNode763.addConnections(webNode762);
		webNode763.addConnections(webNode764);
		webNode764.addConnections(webNode763);
		
		webNode02200.addConnections(webNode02201);
		webNode02201.addConnections(webNode02200);
		webNode02201.addConnections(webNode02202);
		webNode022010.addConnections(webNode022011);
		webNode022010.addConnections(webNode02209);
		webNode022011.addConnections(webNode022010);
		webNode022011.addConnections(webNode022012);
		webNode022012.addConnections(webNode022011);
		webNode02202.addConnections(webNode02201);
		webNode02202.addConnections(webNode02203);
		webNode02203.addConnections(webNode02202);
		webNode02203.addConnections(webNode02204);
		webNode02204.addConnections(webNode02203);
		webNode02204.addConnections(webNode02205);
		webNode02205.addConnections(webNode02204);
		webNode02205.addConnections(webNode02206);
		webNode02206.addConnections(webNode02205);
		webNode02206.addConnections(webNode02207);
		webNode02207.addConnections(webNode02206);
		webNode02207.addConnections(webNode02208);
		webNode02208.addConnections(webNode02207);
		webNode02208.addConnections(webNode02209);
		webNode02209.addConnections(webNode022010);
		webNode02209.addConnections(webNode02208);
		
		webNode1220.addConnections(webNode1221);
		webNode1221.addConnections(webNode1220);
		webNode1221.addConnections(webNode1222);
		webNode1222.addConnections(webNode1221);
		webNode1222.addConnections(webNode1223);
		webNode1223.addConnections(webNode1222);
		
		webNode9970.addConnections(webNode662);
		webNode662.addConnections(webNode9970);
		webNode9970.addConnections(webNode9971);
		webNode9971.addConnections(webNode9970);
		webNode9971.addConnections(webNode9972);
		webNode9972.addConnections(webNode9971);
		webNode9972.addConnections(webNode9973);
		webNode9973.addConnections(webNode9972);
		
		webNode88750.addConnections(Walking.getWebPathFinder().getNearest(new Tile(3548, 3534), 2));
		Walking.getWebPathFinder().getNearest(new Tile(3548, 3534), 2).addConnections(webNode88750);
		webNode88750.addConnections(webNode88751);
		webNode88751.addConnections(webNode88750);
		webNode88751.addConnections(webNode88752);
		webNode88752.addConnections(webNode88751);
		webNode88752.addConnections(webNode88753);
		webNode88753.addConnections(webNode88752);
		
		webNode5660.addConnections(Walking.getWebPathFinder().getNearest(new Tile(2909, 3545), 2));
		Walking.getWebPathFinder().getNearest(new Tile(2909, 3545), 2).addConnections(webNode5660);
		webNode5660.addConnections(webNode5661);
		webNode5661.addConnections(webNode5660);
		webNode5661.addConnections(webNode5662);
		webNode5662.addConnections(webNode5661);
		
		webNode66550.addConnections(Walking.getWebPathFinder().getNearest(new Tile(2923, 3567), 2));
		Walking.getWebPathFinder().getNearest(new Tile(2923, 3567), 2).addConnections(webNode66550);
		webNode66550.addConnections(webNode66551);
		webNode66551.addConnections(webNode66550);
		
		webNode524170.addConnections(Walking.getWebPathFinder().getNearest(new Tile(2529, 3498), 2));
		Walking.getWebPathFinder().getNearest(new Tile(2529, 3498), 2).addConnections(webNode524170);
		webNode524170.addConnections(webNode524171);
		webNode524171.addConnections(webNode524170);
		webNode524171.addConnections(webNode524172);
		webNode524172.addConnections(webNode524171);
		
		WebFinder webFinder = Walking.getWebPathFinder();
		
		AbstractWebNode[] webNodes = {webNode823922, webNode823921, webNode823920, webNode524172, webNode524171, webNode524170, webNode66551, webNode66550, webNode5662, webNode5661, webNode5660, webNode88753, webNode88752, webNode88751, webNode88750, webNode1223, webNode1222, webNode1221, webNode1220, webNode022012, webNode022011, webNode022010, webNode02209, webNode02208, webNode02207, webNode02206, webNode02205, webNode02204, webNode02203, webNode02202, webNode02201, webNode02200, webNode764, webNode763, webNode762, webNode761, webNode760, webNode8811, webNode8810, webNode889, webNode888, webNode887, webNode886, webNode885, webNode884, webNode883, webNode882, webNode881, webNode880, webNode6611, webNode6610, webNode666, webNode667, webNode669, webNode665, webNode668, webNode660, webNode664, webNode661, webNode663, webNode662, webNode336, webNode337, webNode335, webNode330, webNode331, webNode334, webNode332, webNode333, webNode338, webNode339, webNode3310, webNode3311, webNode3312, webNode3313, webNode3314, webNode3315, webNode3316, webNode997, webNode996, webNode995, webNode994, webNode993, webNode992, webNode991, webNode990, webNode14, webNode13, webNode12, webNode11, webNode10, webNode9, webNode8, webNode7, webNode9973, webNode9972, webNode9971, webNode9970};
		for (AbstractWebNode webNode : webNodes) {
			sleep(50); //this is probably placebo but it seems to work better with a small sleep
			webFinder.addWebNode(webNode);
		}
		
		dbbotname = getLocalPlayer().getName();
		dbbotworld = Client.getCurrentWorld();
		dbbottask = "Gearing up";
		dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
		onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
	
		if (Combat.isAutoRetaliateOn() == true) {
			sleep(randomNum(100,300));
            Combat.toggleAutoRetaliate(false);
            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
			sleep(randomNum(100,300));
		}
		
		if (Tabs.getOpen() != Tab.OPTIONS) {
			Tabs.open(Tab.OPTIONS);
			sleepUntil(() -> Tabs.getOpen() == Tab.OPTIONS, randomNum(1000, 2000));
			sleep(randomNum(200,400));
		}
		
		if (Widgets.getWidget(261).getChild(1).getChild(0).getTextureId() != 762) {
			Widgets.getWidget(261).getChild(1).getChild(0).interact("Display");
			sleep(randomNum(200,400));
		}
		
		int zoomx = Widgets.getWidget(261).getChild(10).getX();
		Random rx = new Random();
		double zoomxnormalclick = rx.nextGaussian()*3+zoomx;
		int zoomy = Widgets.getWidget(261).getChild(10).getY();
		Random rx1 = new Random();
		double zoomynormalclick = rx1.nextGaussian()*2+zoomy;
		Mouse.click(new Point((int) zoomxnormalclick+20, (int) zoomynormalclick+7));
		sleep(randomNum(200,400));
		
		if (!contains(membersworlds, Client.getCurrentWorld())) {
			WorldHopper.hopWorld(membersworlds[randomNum(0,membersworlds.length)]);
			sleepUntil(() -> dbbotworld != Client.getCurrentWorld(), randomNum(23000, 30000));
			while (!Tabs.isOpen(Tab.INVENTORY)) {
				Tabs.open(Tab.INVENTORY);
				Random r = new Random();
				double sleepr = r.nextGaussian() * 10 + 103;
				sleep((int) sleepr);
			}
			sleep(randomNum(250, 420));
		}
		
	}

	//When script ends do this.
	public void onExit() {
		log("Bot Ended");
		dbbotname = getLocalPlayer().getName();
		dbbotworld = Client.getCurrentWorld();
		dbbottask = "Ending";
		dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
		dbbotonline = 0;
		onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
	}

	public int randomNum(int i, int k) {
		int num = (int)(Math.random() * (k - i + 1)) + i;
		return num;
	}
	
	public void onlineBotUpdate(String dbbotnameparam, int dbbotworldparam, String dbbottaskparam, String dbbotruntimeparam, int dbbotrangerbootsparam, int dbbotcluestotalparam, int dbbotcluesperhourparam, int dbbotonlineparam) {
		try{
	       String urlParameters = "botname="+dbbotnameparam+"&botworld="+dbbotworldparam+"&bottask="+dbbottaskparam+"&botruntime="+dbbotruntimeparam+"&botrangerboots="+dbbotrangerbootsparam+"&botcluestotal="+dbbotcluestotalparam+"&botcluesperhour="+dbbotcluesperhourparam+"&botonline="+dbbotonlineparam; 
	       URL url = new URL("http://83.85.215.128:8080/dbupdate.php");
	       HttpURLConnection hc = (HttpURLConnection)url.openConnection();
	       hc.setDoInput(true);
	       hc.setDoOutput(true);
	       hc.setInstanceFollowRedirects(false);
	       hc.setRequestMethod("POST");
	       hc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
	       hc.setRequestProperty("charset", "utf-8");
	       hc.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
	       hc.setUseCaches(false);
	       DataOutputStream ds = new DataOutputStream(hc.getOutputStream());
	       ds.writeBytes(urlParameters);
	       ds.flush();
	       //String line;
	       BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream()));
	       //line = br.readLine();
	       //log(line);
	       ds.close();
	       br.close();
	       hc.disconnect();
	    } catch(IOException e){
	    	log("couldnt reach status update server");
	    }    
	 }	
		
	public void onlineBotInsert(String dbbotnameparam, int dbbotworldparam, String dbbottaskparam, String dbbotruntimeparam, int dbbotrangerbootsparam, int dbbotcluestotalparam, int dbbotcluesperhourparam, int dbbotonlineparam) {
		try{
		   String urlParameters = "botname="+dbbotnameparam+"&botworld="+dbbotworldparam+"&bottask="+dbbottaskparam+"&botruntime="+dbbotruntimeparam+"&botrangerboots="+dbbotrangerbootsparam+"&botcluestotal="+dbbotcluestotalparam+"&botcluesperhour="+dbbotcluesperhourparam+"&botonline="+dbbotonlineparam; 
	       URL url = new URL("http://83.85.215.128:8080/dbinsert.php");
	       HttpURLConnection hc = (HttpURLConnection)url.openConnection();
	       hc.setDoInput(true);
	       hc.setDoOutput(true);
	       hc.setInstanceFollowRedirects(false);
	       hc.setRequestMethod("POST");
	       hc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
	       hc.setRequestProperty("charset", "utf-8");
	       hc.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
	       hc.setUseCaches(false);
	       DataOutputStream ds = new DataOutputStream(hc.getOutputStream());
	       ds.writeBytes(urlParameters);
	       ds.flush();
	       //String line;
	       BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream()));
	       //line = br.readLine();
	       //log(line);
	       ds.close();
	       br.close();
	       hc.disconnect();
	    } catch(IOException e){
	    	log("couldnt reach status update server");
	    }
	 }
	
	public void telegramSendMessage(String message, int chatID) {
		try{
		   String urlParameters = chatID + "&text=" + message; 
	       URL url = new URL("https://api.telegram.org/bot1328695142:AAGPdSpFKkTvdaFTIOqWTC63OX-1QJLi6YE/sendMessage?chat_id="+urlParameters);
	       HttpURLConnection hc = (HttpURLConnection)url.openConnection();
	       hc.setDoInput(true);
	       hc.setDoOutput(true);
	       hc.setInstanceFollowRedirects(false);
	       hc.setUseCaches(false);
	       DataOutputStream ds = new DataOutputStream(hc.getOutputStream());
	       ds.writeBytes(urlParameters);
	       ds.flush();
	       //String line;
	       BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream()));
	       //line = br.readLine();
	       //log(line);
	       ds.close();
	       br.close();
	       hc.disconnect();
	    } catch(IOException e){
	    	log("couldnt reach telegram api");
	    }
	 }

	public static boolean contains(final int[] array, final int v) {

        boolean result = false;

        for(int i : array){
            if(i == v){
                result = true;
                break;
            }
        }

        return result;
    }
	
	public void C19740() {
		currentClue = 19740;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!arceuustodarkessencemine.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!arceuustodarkessencemine.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.ARCEUUS_LIBRARY);
					sleepUntil(() -> arceuuslibraryteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (arceuustodarkessencemine.contains(getLocalPlayer()) && !darkessenceminebig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(darkessenceminesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
	
		if (darkessenceminebig.contains(getLocalPlayer())) {
			NPC clerris = NPCs.closest("Clerris"); 
			if (clerris != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					clerris.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("738");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					clerris.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10256() {
		currentClue = 10256;
        currentCluestr = Integer.toString(currentClue);
        
        if (previousClue != currentClue) {
            if (Walking.isRunEnabled() == false) {
                Walking.toggleRun();
            }
            
            if (Combat.isAutoRetaliateOn() == true) {
                sleep(randomNum(100,300));
                Combat.toggleAutoRetaliate(false);
                sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
                sleep(randomNum(100,300));
            }
            
            dbbotcluestotal ++;
            timeBeganClue = System.currentTimeMillis();
            
            dbbotworld = Client.getCurrentWorld();
            dbbottask = "Clue "+currentCluestr;
            dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
            onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
        }
        
        if ((getLocalPlayer().getX() <= 3496 || getLocalPlayer().getX() >= 3600) && (getLocalPlayer().getY() <= 9900 || getLocalPlayer().getY() >= 10000)  && !fenkenstrainscastletograveyard.contains(getLocalPlayer()) && !mausoleum.contains(getLocalPlayer()) && !morytaniaundergroundarea.contains(getLocalPlayer())) {
			if (Inventory.contains("Fenkenstrain's castle teleport")) {
				Inventory.interact("Fenkenstrain's castle teleport", "Break");
				sleepUntil(() -> fenkenstrainscastleteleport.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
        }

        if (fenkenstrainscastletograveyard.contains(getLocalPlayer()) && !mausoleum.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) && !morytaniaundergroundarea.contains(getLocalPlayer())) {
			GameObject memorial = GameObjects.closest(f -> f.getName().contentEquals("Memorial") && memorialarea.contains(f));
			if (memorial != null && memorial.getTile().distance() <= 14) {
				memorial.interact("Push");
				sleepUntil(() -> morytaniaundergroundarea.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
        	
        	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(fenkenstrainsgraveyardsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
        
        if (!fenkenstrainscastletograveyard.contains(getLocalPlayer()) && !mausoleum.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) && morytaniaundergroundarea.contains(getLocalPlayer())) {
        	GameObject entrance = GameObjects.closest(f -> f.getName().contentEquals("Entrance"));
			if (entrance != null && entrance.getTile().distance() <= 14 && !undergroundafterentrance.contains(getLocalPlayer())) {
				entrance.interact("Open");
				sleepUntil(() -> undergroundpastentrance.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			GameObject ladder = GameObjects.closest(f -> f.getName().contentEquals("Ladder") && morytaniaundergroundladdertile.contains(f));
			if (ladder != null && ladder.getTile().distance() <= 14) {
				ladder.interact("Climb-up");
				sleepUntil(() -> mausoleum.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
        	
        	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (undergroundafterentrance.contains(getLocalPlayer())) {
				Walking.walk(morytaniaundergroundladder.getRandomTile());
				sleep(randomNum(200, 400));
			} else if (!undergroundafterentrance.contains(getLocalPlayer())) {
				Walking.walk(undergroundbeforeentrance.getCenter());
				sleep(randomNum(200, 400));
			}
		}
        
        if (!Inventory.contains("Maple longbow") && !Equipment.contains("Maple longbow") && mausoleum.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Maple longbow") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int maplelongbow = 0;
		int mithrilplateskirt = 0;
		
		if (Inventory.contains("Maple longbow") && !Equipment.contains("Maple longbow") && mausoleum.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			maplelongbow = Inventory.slot(f -> f.getName().contains("Maple longbow"));
			Inventory.slotInteract(maplelongbow, "Wield");
			sleepUntil(() -> Equipment.contains("Maple longbow"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			mithrilplateskirt = Inventory.slot(f -> f.getName().contains("Mithril plateskirt"));
			Inventory.slotInteract(mithrilplateskirt, "Wear");
			sleepUntil(() -> Equipment.contains("Mithril plateskirt"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			if (Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(200,400));
			}
			Equipment.interact(Equipment.getSlotForItem(f -> f.getName().contains("Boots of lightness")), "Remove");
			sleepUntil(() -> !Equipment.contains("Boots of lightness"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Maple longbow") && Equipment.contains("Maple longbow") && mausoleum.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.PANIC);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.WAVE);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Maple longbow") && Equipment.contains("Maple longbow")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Maple longbow") || Equipment.contains("Maple longbow")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
	
					while (!Equipment.contains("Boots of lightness") && Inventory.contains("Boots of lightness")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.slotInteract(maplelongbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Maple longbow"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.slotInteract(mithrilplateskirt, "Wear");
						sleepUntil(() -> !Equipment.contains("Mithril plateskirt"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Boots of lightness", "Wear");
						sleepUntil(() -> Equipment.contains("Boots of lightness"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Maple longbow") && !Equipment.contains("Maple longbow")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Maple longbow") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
        
        if (Inventory.contains("Reward casket (medium)")) {
            setupcomplete = 0;
            sleep(randomNum(300,600));
        } else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
            log("stuck on clue: " + currentClue);
            if (Inventory.contains("Clue scroll (medium)")) {
                sleep(randomNum(400,700));
                Inventory.drop("Clue scroll (medium)");
                sleep(randomNum(300,600));
            }
            backing = 1;
            setupcomplete = 0;
        }
        
        previousClue = currentClue;
	}
	
	public void C10260() {
		currentClue = 10260;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!fairyrintotaibwowannai.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!fairyrintotaibwowannai.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SOUTH_TAI_BWO_WANNAI_VILLAGE);
					sleepUntil(() -> CKRteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (fairyrintotaibwowannai.contains(getLocalPlayer()) && !taibwowannaistash.contains(getLocalPlayer()) && !taibwowannaifence.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(5,7))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(taibwowannaistashsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Mithril med helm") && !Equipment.contains("Mithril med helm") && taibwowannaistash.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Mithril med helm") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int mithrilmedhelm = 0;
		int greendhidechaps = 0;
		int ringofduelingemote = 0;
		
		if (Inventory.contains("Mithril med helm") && !Equipment.contains("Mithril med helm") && taibwowannaistash.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			mithrilmedhelm = Inventory.slot(f -> f.getName().contains("Mithril med helm"));
			Inventory.slotInteract(mithrilmedhelm, "Wear");
			sleepUntil(() -> Equipment.contains("Mithril med helm"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			greendhidechaps = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
			Inventory.slotInteract(greendhidechaps, "Wear");
			sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			ringofduelingemote = Inventory.slot(f -> f.getName().contains("Ring of dueling(8)"));
			Inventory.slotInteract(ringofduelingemote, "Wear");
			sleepUntil(() -> !Equipment.contains("Ring of dueling(8)"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Mithril med helm") && Equipment.contains("Mithril med helm") && !taibwowannaifence.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(2,3))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(taibwowannaifencesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Mithril med helm") && Equipment.contains("Mithril med helm") && taibwowannaifence.contains(getLocalPlayer())) {
			sleep(randomNum(60, 150));
			if(Tabs.getOpen() != Tab.EMOTES) {
				Tabs.open(Tab.EMOTES);
				sleep(randomNum(73,212));
			}
			
			Emotes.doEmote(Emote.BECKON);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));

			int scrollx = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx+(scrollwidth/2)+randomNum(1,4), scrolly+(scrollheight/2)+randomNum(1,4)));
			sleep(randomNum(60, 150));

			Widgets.getWidget(216).getChild(1).getChild(20).interact("Clap");
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			int scrollx1 = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly1 = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight1 = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth1 = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx1+(scrollwidth1/2)+randomNum(1,4), scrolly1+(scrollheight1/8)+randomNum(1,4)));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Mithril med helm") && Equipment.contains("Mithril med helm")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Mithril med helm") || Equipment.contains("Mithril med helm")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (!Inventory.contains("Mithril med helm") && Equipment.contains("Mithril med helm")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> !Equipment.contains("Mithril med helm"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int ringofwealth = Inventory.slot(f -> f.getName().contains("Ring of wealth"));
						Inventory.slotInteract(ringofwealth, "Wear");
						sleepUntil(() -> !Equipment.contains("Ring of dueling(8)"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Mithril med helm") && !Equipment.contains("Mithril med helm")) {
						Walking.walk(taibwowannaistashsmall.getRandomTile());
						sleep(randomNum(700, 850));
						sleepUntil(() ->  !getLocalPlayer().isMoving(), randomNum(6000, 7000));
						sleep(randomNum(100, 200));
						
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Mithril med helm") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10262() {
		currentClue = 10262;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!Inventory.contains("Ruby amulet") && !Equipment.contains("Ruby amulet") && !castlewarslobby.contains(getLocalPlayer())) {
			int ringofduelingslot = Inventory.slot(f -> f.getName().contains("Ring of dueling"));
			if (Inventory.contains(f -> f.getName().contains("Ring of dueling"))) {
				if(Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(73,212));
				}
				Inventory.slotInteract(ringofduelingslot, "Rub");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.chooseOption(2);
				sleepUntil(() -> castlewarslobby.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(130, 300));
			}
		}
		
		if (!Inventory.contains("Ruby amulet") && !Equipment.contains("Ruby amulet") && castlewarslobby.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Ruby amulet") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int rubyamulet = 0;
		int mithrilscim = 0;
		int teamcape = 0;
		
		if (Inventory.contains("Ruby amulet") && !Equipment.contains("Ruby amulet") && castlewarslobby.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			rubyamulet = Inventory.slot(f -> f.getName().contains("Ruby amulet"));
			Inventory.slotInteract(rubyamulet, "Wear");
			sleepUntil(() -> Equipment.contains("Ruby amulet"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			mithrilscim = Inventory.slot(f -> f.getName().contains("Mithril scimitar"));
			Inventory.slotInteract(mithrilscim, "Wield");
			sleepUntil(() -> Equipment.contains("Mithril scimitar"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			teamcape = Inventory.slot(f -> f.getName().contains("Team-"));
			Inventory.slotInteract(teamcape, "Wear");
			sleepUntil(() -> !Equipment.contains("Green cape"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Ruby amulet") && Equipment.contains("Ruby amulet") && castlewarslobby.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.YAWN);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.SHRUG);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Ruby amulet") && Equipment.contains("Ruby amulet")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Equipment.contains("Mithril scimitar") || Inventory.contains("Mithril scimitar")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (!Equipment.contains("Green cape") && !Inventory.contains("Mithril scimitar")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.slotInteract(rubyamulet, "Wear");
						sleepUntil(() -> !Equipment.contains("Ruby amulet"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.slotInteract(mithrilscim, "Wield");
						sleepUntil(() -> !Equipment.contains("Mithril scimitar"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.slotInteract(teamcape, "Wear");
						sleepUntil(() -> Equipment.contains("Green cape"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (!Equipment.contains("Mithril scimitar") && Inventory.contains("Mithril scimitar")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Ruby amulet") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10270() {
		currentClue = 10270;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!castlewarstoobservatory.contains(getLocalPlayer())) {
			int ringofduelingslot = Inventory.slot(f -> f.getName().contains("Ring of dueling"));
			if (Inventory.contains(f -> f.getName().contains("Ring of dueling"))) {
				if(Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(73,212));
				}
				Inventory.slotInteract(ringofduelingslot, "Rub");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(400,600));
				Dialogues.chooseOption(2);
				sleepUntil(() -> castlewarslobby.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(130, 300));
			}
		}
		
		if (castlewarstoobservatory.contains(getLocalPlayer()) && !observatory.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(observatoryropeshortcut.getRandomTile());
			sleep(randomNum(200,400));
			
			GameObject ropeobservatoryagility = GameObjects.closest("Rope");
			if (ropeobservatoryagility != null && ropeobservatoryagility.getTile().distance() <= 15) {
				ropeobservatoryagility.interact("Climb");
				sleepUntil(() -> observatory.contains(getLocalPlayer()), randomNum(12300, 14500));
				sleep(randomNum(1450, 2300));
			}
		}

		if (!Inventory.contains("Ruby amulet") && !Equipment.contains("Ruby amulet") && observatory.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Ruby amulet") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int rubyamulet1 = 0;
		int mithrilchainbody = 0;
		int greendhidechaps = 0;
		
		if (Inventory.contains("Ruby amulet") && !Equipment.contains("Ruby amulet") && observatory.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			rubyamulet1 = Inventory.slot(f -> f.getName().contains("Ruby amulet"));
			Inventory.slotInteract(rubyamulet1, "Wear");
			sleepUntil(() -> Equipment.contains("Ruby amulet"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			mithrilchainbody = Inventory.slot(f -> f.getName().contains("Mithril chainbody"));
			Inventory.slotInteract(mithrilchainbody, "Wear");
			sleepUntil(() -> Equipment.contains("Mithril chainbody"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			greendhidechaps = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
			Inventory.slotInteract(greendhidechaps, "Wear");
			sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Ruby amulet") && Equipment.contains("Ruby amulet") && observatory.contains(getLocalPlayer()) && !middleofobservatory.contains(getLocalPlayer())) {
			Walking.walkExact(middleofobservatory.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Ruby amulet") && Equipment.contains("Ruby amulet") && observatory.contains(getLocalPlayer()) && middleofobservatory.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.THINK);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.SPIN);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Ruby amulet") && Equipment.contains("Ruby amulet")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Equipment.contains("Mithril chainbody") || Inventory.contains("Mithril chainbody")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
				
					while (Equipment.contains("Mithril chainbody") && !Inventory.contains("Mithril chainbody")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int necklaceofpassage = Inventory.slot(f -> f.getName().contains("Necklace of passage"));
						Inventory.slotInteract(necklaceofpassage, "Wear");
						sleepUntil(() -> !Equipment.contains("Ruby amulet"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> !Equipment.contains("Mithril chainbody"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (!Equipment.contains("Mithril chainbody") && Inventory.contains("Mithril chainbody")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Ruby amulet") && !getLocalPlayer().isAnimating(), randomNum(7500,9000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12043() {
		currentClue = 12043;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!draynormanortodigspot.contains(getLocalPlayer())) {
			if (Inventory.contains("Draynor manor teleport")) {
				Inventory.interact("Draynor manor teleport", "Break");
				sleepUntil(() -> draynormanortp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (draynormanortodigspot.contains(getLocalPlayer()) && !digspot12043.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(digspot12043.getCenter());
			sleep(randomNum(200,400));
		}
		
		if (draynormanortodigspot.contains(getLocalPlayer()) && digspot12043.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12045() {
		currentClue = 12045;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto12045.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto12045.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("2");
				sleepUntil(() -> treegnomestrongholdspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !digspot12045.contains(getLocalPlayer()) && treegnomestrongholdto12045.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (getLocalPlayer().getX() >= 2401) {
				Walking.walk(new Tile(randomNum(2397, 2389), randomNum(3423, 3424)));
				sleep(randomNum(200, 400));
			}
			if (getLocalPlayer().getX() < 2401) {
				Walking.walkExact(digspot12045.getCenter());
				sleep(randomNum(200, 400));
			}
		}
		
		if (digspot12045.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12051() {
		currentClue = 12051;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!digsitetoexamcentre.contains(getLocalPlayer())) {
			if (Inventory.contains("Digsite teleport")) {
				Inventory.interact("Digsite teleport", "Teleport");
				sleepUntil(() -> digsiteteleport.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (digsitetoexamcentre.contains(getLocalPlayer()) && !digspot12051.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			Walking.walkExact(digspot12051.getCenter());
			sleep(randomNum(200,400));
		}
		
		if (digsitetoexamcentre.contains(getLocalPlayer()) && digspot12051.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12053() {
		currentClue = 12053;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!CKStoslayertower.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!CKStoslayertower.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.CANIFIS);
					sleepUntil(() -> CKSteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (CKStoslayertower.contains(getLocalPlayer()) && !slayertowerdigspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(slayertowerdigspot.getCenter());
			sleep(randomNum(200, 400));
		}

		if (slayertowerdigspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12063() {
		currentClue = 12063;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!rimmingtonboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && !rimmingtonporttobushpatch.contains(getLocalPlayer()) && !rimmingtonbushpatch.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && !rimmingtonporttobushpatch.contains(getLocalPlayer()) && !rimmingtonbushpatch.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ardougneportsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && ardougneport.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !rimmingtonporttobushpatch.contains(getLocalPlayer()) && !rimmingtonbushpatch.contains(getLocalPlayer())) {
			NPC capbarnaby = NPCs.closest("Captain Barnaby"); 
			if (capbarnaby != null) {
				capbarnaby.interact("Rimmington");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(6500, 8500));
				sleep(randomNum(400,700));
			}
		}

		if (rimmingtonboats.contains(getLocalPlayer())) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && rimmingtonporttobushpatch.contains(getLocalPlayer()) && !rimmingtonbushpatch.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(rimmingtonbushpatchsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}

		if (!rimmingtonboats.contains(getLocalPlayer()) && rimmingtonbushpatch.contains(getLocalPlayer())) {
			NPC Taria = NPCs.closest("Taria"); 
			if (Taria != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					Taria.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("7");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					Taria.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12067() {
		currentClue = 12067;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			C12067teleportbugfix = 0;
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!catherbytocamelot.contains(getLocalPlayer())) {
			if (Inventory.contains("Camelot teleport") && C12067teleportbugfix == 0) {
				Inventory.interact("Camelot teleport", "Break");
				sleepUntil(() -> camelotteleport.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(730, 900));
				if (catherbytocamelot.contains(getLocalPlayer())) {
					C12067teleportbugfix = 1;
				}
			}
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && !C12067area.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(C12067area.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (C12067area.contains(getLocalPlayer())) {
			NPC hickton = NPCs.closest("Hickton"); 
			if (hickton != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					hickton.interact("Talk-to");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					sleep(randomNum(300, 450));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Keyboard.type("2");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					C12067teleportbugfix = 0;
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					hickton.interact("Talk-to");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19744() {
		currentClue = 19744;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (teleported == 0 && !portpascariliusbig.contains(getLocalPlayer()) && !dockmasterhouse.contains(getLocalPlayer()) && Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !draynorvillagetoportsarim.contains(getLocalPlayer())) {
			teleported = 1;
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(3);
			sleepUntil(() -> draynorvillagetp.contains(getLocalPlayer()), randomNum(4500,6000));
			if (!draynorvillagetp.contains(getLocalPlayer())) {
				teleported = 0;
			}
			sleep(randomNum(120, 300));
		}
		
		if (!portpascariliusbig.contains(getLocalPlayer()) && !dockmasterhouse.contains(getLocalPlayer()) && draynorvillagetoportsarim.contains(getLocalPlayer()) && !veosbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(veosnode.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!portpascariliusbig.contains(getLocalPlayer()) && !dockmasterhouse.contains(getLocalPlayer()) && veosbig.contains(getLocalPlayer())) {
			NPC veos = NPCs.closest("Veos"); 
			if (veos != null) {
				veos.interact("Port Piscarilius");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(5500, 6400));
				sleep(randomNum(400, 700));
			}
		}
		
		
		if (getLocalPlayer().getZ() == 1) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				sleep(randomNum(2000, 3000));
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			if (getLocalPlayer().getZ() == 0) {
				teleported = 0;
			}
		}
		
		if (portpascariliusbig.contains(getLocalPlayer()) && !dockmasterhouse.contains(getLocalPlayer()) && !draynorvillagetoportsarim.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(dockmasterhouse.getCenter());
			sleep(randomNum(200, 400));

		}
		
		if (portpascariliusbig.contains(getLocalPlayer()) && dockmasterhouse.contains(getLocalPlayer()) && !draynorvillagetoportsarim.contains(getLocalPlayer())) {
			NPC dockmaster = NPCs.closest("Dockmaster"); 
			if (dockmaster != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					dockmaster.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("5");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					dockmaster.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19752() {
		currentClue = 19752;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!camelottoflaxkeeper.contains(getLocalPlayer()) && !flaxkeeperareabig.contains(getLocalPlayer())) {
			if (Inventory.contains("Camelot teleport")) {
				Inventory.interact("Camelot teleport", "Break");
				sleepUntil(() -> camelotteleport.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!flaxkeeperareabig.contains(getLocalPlayer()) && camelottoflaxkeeper.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(flaxkeeperareasmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (flaxkeeperareabig.contains(getLocalPlayer())) {
			NPC flaxkeeper = NPCs.closest("Flax keeper"); 
			if (flaxkeeper != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					flaxkeeper.interact("Talk-to");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(380, 450));
					Dialogues.continueDialogue();
					sleep(randomNum(480, 650));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Keyboard.type("676");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
				}else if (!Inventory.contains("Challenge scroll (medium)")) {
					flaxkeeper.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19756() {
		currentClue = 19756;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Skills necklace(")) && !woodcuttingguildtohosidius.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int skillsneck = Inventory.slot(f -> f.getName().contains("Skills necklace("));
			Inventory.slotInteract(skillsneck, "Rub");
			sleep(randomNum(645,800));
			Widgets.getWidget(187).getChild(3).getChild(4).interact("Continue");
			sleepUntil(() -> woodcuttingguildtp.contains(getLocalPlayer()), randomNum(3500,4800));
			sleep(randomNum(745,900));
		}
		
		if (woodcuttingguildtohosidius.contains(getLocalPlayer()) && !allotmentpatchhosidius.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(allotmentpatchhosidiusmiddle.getRandomTile());
			sleep(randomNum(300, 500));
		}

		if (allotmentpatchhosidius.contains(getLocalPlayer())) {
			NPC Marisi = NPCs.closest("Marisi"); 
			if (Marisi != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					Marisi.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(345,500));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("5");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					Marisi.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19764() {
		currentClue = 19764;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!wizardtowerbigfloor2.contains(getLocalPlayer()) && !wizardtowerbigfloor1.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (wizardtowerbig.contains(getLocalPlayer()) && !wizardtowerstaircaseroom.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,4))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(wizardtowerinsidestaircaseroomsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (wizardtowerbigfloor2.contains(getLocalPlayer())) {
			GameObject stairs = GameObjects.closest(f -> f.getName().contentEquals("Staircase") && f.hasAction("Climb-down"));
			if (stairs != null) {
				int currentfloor = getLocalPlayer().getZ();
				stairs.interact("Climb-down");
				sleepUntil(() -> getLocalPlayer().getZ() != currentfloor, randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (wizardtowerinside.contains(getLocalPlayer()) && getLocalPlayer().getZ() < 1 && wizardtowerstaircaseroom.contains(getLocalPlayer())) {
			GameObject stairs = GameObjects.closest(f -> f.getName().contentEquals("Staircase") && f.hasAction("Climb-up"));
			if (stairs != null) {
				int currentfloor = getLocalPlayer().getZ();
				stairs.interact("Climb-up");
				sleepUntil(() -> getLocalPlayer().getZ() != currentfloor, randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (wizardtowerbigfloor1.contains(getLocalPlayer()) && getLocalPlayer().getZ() == 1 && !traibornroom.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(2, 3))) {
			Walking.walk(traibornroomsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (wizardtowerbigfloor1.contains(getLocalPlayer()) && getLocalPlayer().getZ() == 1 && traibornroom.contains(getLocalPlayer())) {
			NPC traiborn = NPCs.closest("Traiborn"); 
			if (traiborn != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					traiborn.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("3150");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					traiborn.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19776() {
		currentClue = 19776;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (Inventory.contains(f -> f.getName().contains("Skills necklace(")) && !woodcuttingguildtoshayzienring.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int skillsneck = Inventory.slot(f -> f.getName().contains("Skills necklace("));
			Inventory.slotInteract(skillsneck, "Rub");
			sleep(randomNum(845,930));
			Widgets.getWidget(187).getChild(3).getChild(4).interact("Continue");
			sleepUntil(() -> woodcuttingguildtp.contains(getLocalPlayer()), randomNum(3500,4800));
			sleep(randomNum(745,900));
		}

		if (woodcuttingguildtoshayzienring.contains(getLocalPlayer()) && !shayzienringssurrounding.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (!getLocalPlayer().isMoving() && woodcuttingguildtp.contains(getLocalPlayer())) {
				Walking.walk(webnodebugfixareawoodcuttingguild.getRandomTile());
				sleep(randomNum(300, 500));
			} else {
				if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
					int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
					if (Tabs.getOpen() != Tab.INVENTORY) {
						Tabs.open(Tab.INVENTORY);
						sleep(randomNum(200,400));
					}
					Inventory.slotInteract(stampot, "Drink");
					sleep(randomNum(200,300));
				}
				
				Walking.walk(shayzienringstashunit.getRandomTile());
				sleep(randomNum(300, 500));
			}	
		}
		
		if (!Inventory.contains("Adamant full helm") && !Equipment.contains("Adamant full helm") && shayzienringstashunitbig.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Adamant full helm") && !getLocalPlayer().isAnimating(), randomNum(4500,6000));
				sleep(randomNum(600, 800));
			}
		}
		
		int adamantfullhelm = 0;
		int adamantplatebody = 0;
		int adamantplatelegs = 0;
		
		if (Inventory.contains("Adamant full helm") && !Equipment.contains("Adamant full helm") && shayzienringstashunitbig.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			adamantfullhelm = Inventory.slot(f -> f.getName().contains("Adamant full helm"));
			Inventory.slotInteract(adamantfullhelm, "Wear");
			sleepUntil(() -> Equipment.contains("Adamant full helm"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			adamantplatebody = Inventory.slot(f -> f.getName().contains("Adamant platebody"));
			Inventory.slotInteract(adamantplatebody, "Wear");
			sleepUntil(() -> Equipment.contains("Adamant platebody"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			adamantplatelegs = Inventory.slot(f -> f.getName().contains("Adamant platelegs"));
			Inventory.slotInteract(adamantplatelegs, "Wear");
			sleepUntil(() -> !Equipment.contains("Adamant platelegs"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
								
		if (shayzienringssurrounding.contains(getLocalPlayer()) && !shayzienringmiddle.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(1,2))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(shayzienringmiddle.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Adamant full helm") && Equipment.contains("Adamant full helm") && shayzienringmiddle.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.BECKON);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.ANGRY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri"); 
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Adamant full helm") && Equipment.contains("Adamant full helm")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Adamant platelegs") || Equipment.contains("Adamant platelegs")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (!Inventory.contains("Adamant platelegs") && Equipment.contains("Adamant platelegs")) {
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> !Equipment.contains("Adamant full helm"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> !Equipment.contains("Adamant platebody"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> Equipment.contains("Adamant platelegs"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					sleep(randomNum(20,50));
	
					while (Inventory.contains("Adamant platelegs") && !Equipment.contains("Adamant platelegs")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null && Inventory.contains("Adamant full helm")) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Adamant full helm") && !getLocalPlayer().isAnimating(), randomNum(7500,9000));
							sleep(randomNum(500, 800));
						} else if (STASH == null && Inventory.contains("Adamant full helm")) {
							Walking.walk(shayzienringstashunit.getRandomTile());
							sleep(randomNum(600, 800));
							sleepUntil(() -> !getLocalPlayer().isMoving() && getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(200, 400));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23136() {
		currentClue = 23136;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !karamjatovolcano.contains(getLocalPlayer()) && !karamjaunderground.contains(getLocalPlayer()) && !crandor.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(460, 700));
			Dialogues.chooseOption(2);
			sleepUntil(() -> karamjateleport.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(320, 700));
		}
		
		if (karamjatovolcano.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(karamjaundergroundentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));

			GameObject entrancerocks = GameObjects.closest("Rocks");
			if (entrancerocks != null && entrancerocks.getTile().distance() <= 10) {
				entrancerocks.interact("Climb-down");
				sleepUntil(() -> karamjaunderground.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (karamjaunderground.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			if (getLocalPlayer().getY() <= 9593) {
				Walking.walk(crandorentranceareadoor.getRandomTile());
				sleep(randomNum(200, 400));
			}
			
			if (getLocalPlayer().getY() > 9593) {
				Walking.walk(crandorentranceareasmall.getRandomTile());
				sleep(randomNum(200, 400));
			}

			GameObject climbingrope = GameObjects.closest(f -> f.getName().contentEquals("Climbing rope") && climbingropecrandor.contains(f));
			if (climbingrope != null && climbingrope.getTile().distance() <= 15) {
				climbingrope.interact("Climb");
				sleepUntil(() -> crandor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}

			if (!pastkaramjadungeondoor.contains(getLocalPlayer())) {
				GameObject karamjadungeondoor = GameObjects.closest(f -> f.getName().contentEquals("Wall"));
				if (karamjadungeondoor != null && karamjadungeondoor.getTile().distance() <= 15) {
					karamjadungeondoor.interact("Open");
					sleepUntil(() -> afterdoor.contains(getLocalPlayer()), randomNum(5300, 6500));
					sleep(randomNum(150, 300));
				} 
			}
			
			if (getLocalPlayer().getHealthPercent() <= 40 && Inventory.contains("Shark")) {
				Inventory.interact("Shark", "Eat");
				sleep(randomNum(200, 400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && crandor.contains(getLocalPlayer()) && !C23136digspot.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(C23136digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (C23136digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23140() {
		currentClue = 23140;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !faladorparkbig.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Falador");
			sleepUntil(() -> faladorparktp.contains(getLocalPlayer()), randomNum(1500,1700));
			sleep(randomNum(932,1240));
		}

		if (faladorparkbig.contains(getLocalPlayer()) && !faladorparkbridge.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(faladorparkbridge.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (faladorparkbridge.contains(getLocalPlayer()) && faladorparkbig.contains(getLocalPlayer())) {
			NPC cecilia = NPCs.closest("Cecilia"); 
			if (cecilia != null) {
				cecilia.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(3500, 4500));
				sleep(randomNum(123,411));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.getOptionIndexContaining("Yes, I have.") != -1, randomNum(3500, 4500));
				sleep(randomNum(115,432));
				Dialogues.chooseOption(1);
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
			}
			
			if (Tabs.getOpen() != Tab.MUSIC) {
				Tabs.open(Tab.MUSIC);
				sleep(randomNum(200,400));
			}

			int scrollx = Widgets.getWidget(239).getChild(4).getChild(0).getX();
			int scrolly = Widgets.getWidget(239).getChild(4).getChild(0).getY();
			int scrollheight = Widgets.getWidget(239).getChild(4).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(239).getChild(4).getChild(0).getWidth();
			//Mouse.click(new Point(randomNum(scrollx+(scrollwidth/2)-1,scrollx+(scrollwidth/2)+1), randomNum((int)(scrolly+(scrollheight/3.1)-1), (int)(scrolly+(scrollheight/3.1)+1))));
			Mouse.click(new Point(scrollx+(scrollwidth/2), (int)(scrolly+(scrollheight/3.1))));
			sleep(randomNum(135,442));
			Widgets.getWidget(239).getChild(3).getChild(286).interact("Play");

			if (cecilia != null) {
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(15, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23143() {
		currentClue = 23143;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !faladorparkbig.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Falador");
			sleepUntil(() -> faladorparktp.contains(getLocalPlayer()), randomNum(1500,1700));
			sleep(randomNum(932,1340));
		}

		if (faladorparkbig.contains(getLocalPlayer()) && !faladorparkbridge.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(faladorparkbridge.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (faladorparkbridge.contains(getLocalPlayer()) && faladorparkbig.contains(getLocalPlayer())) {
			NPC cecilia = NPCs.closest("Cecilia"); 
			if (cecilia != null) {
				cecilia.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(3500, 4500));
				sleep(randomNum(123,411));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.getOptionIndexContaining("Yes, I have.") != -1, randomNum(3500, 4500));
				sleep(randomNum(115,432));
				Dialogues.chooseOption(1);
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
			}
			
			if (Tabs.getOpen() != Tab.MUSIC) {
				Tabs.open(Tab.MUSIC);
				sleep(randomNum(200,400));
			}
			
			int scrollx = Widgets.getWidget(239).getChild(4).getChild(0).getX();
			int scrolly = Widgets.getWidget(239).getChild(4).getChild(0).getY();
			int scrollheight = Widgets.getWidget(239).getChild(4).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(239).getChild(4).getChild(0).getWidth();
			//Mouse.click(new Point(randomNum(scrollx+(scrollwidth/2)-1, scrollx+(scrollwidth/2)+1), randomNum((int)(scrolly+(scrollheight/4.75)-1),(int)(scrolly+(scrollheight/4.75)+1))));
			Mouse.click(new Point(scrollx+(scrollwidth/2), (int)(scrolly+(scrollheight/4.75))));
			sleep(randomNum(135,442));
			Widgets.getWidget(239).getChild(3).getChild(491).interact("Play");

			if (cecilia != null) {
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(15, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2805() {
		currentClue = 2805;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!brimhavenboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && !brimhavenporttopeninsula.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6)) && !brimhavenporttopeninsula.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ardougneportsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && ardougneport.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !brimhavenporttopeninsula.contains(getLocalPlayer())) {
			NPC capbarnaby = NPCs.closest("Captain Barnaby"); 
			if (capbarnaby != null) {
				capbarnaby.interact("Brimhaven");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(6500, 8500));
				sleep(randomNum(400,700));
			}
		}
		
		if (brimhavenboats.contains(getLocalPlayer())) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
				
		if (!brimhavenboats.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9)) && brimhavenporttopeninsula.contains(getLocalPlayer()) && !brimhavenpeninsula.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(WEBWALK2805BRIMHAVEN.getCenter());
			sleep(randomNum(200, 400));
			
			GameObject ropeagility = GameObjects.closest("Ropeswing");
			if (ropeagility != null && ropeagility.getTile().distance() <= 20) {
				ropeagility.interact("Swing-on");
				sleepUntil(() -> brimhavenpeninsula.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}

		if (!brimhavenboats.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(2,5)) && brimhavenpeninsula.contains(getLocalPlayer()) && !C2805area.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer())) {
			Walking.walkExact(C2805area.getCenter());
			sleep(randomNum(200, 400));
			
			if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
				Inventory.interact("Shark", "Eat");
				sleep(randomNum(200, 400));
			}
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && C2805area.contains(getLocalPlayer()) && Inventory.contains("Spade") && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2809() {
		currentClue = 2809;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomevillageto2809.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomevillageto2809.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(7032,1040));
				Keyboard.type("1");
				sleepUntil(() -> treegnomevillagenotthrugate.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}
				
		if (treegnomevillagenotthrugate.contains(getLocalPlayer()) && !treegnomevillagethrugate.contains(getLocalPlayer()) && !grandexchangearea.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject treegnomegate = GameObjects.closest(f -> f.getName().contentEquals("Loose Railing") && f.hasAction("Squeeze-through"));
			if (treegnomegate != null & treegnomevillagegatelarge.contains(getLocalPlayer())) {
				treegnomegate.interact("Squeeze-through");
				sleepUntil(() -> treegnomevillagethrugate.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(treegnomevillagegate.getCenter());
			sleep(randomNum(432,740));
		}
		
		if (treegnomevillagethrugate.contains(getLocalPlayer()) && !treegnomevillagenotthrugate.contains(getLocalPlayer()) && !grandexchangearea.contains(getLocalPlayer())) {
			NPC elkoy = NPCs.closest(f -> f != null && f.getName().contentEquals("Elkoy")); 
			if (elkoy != null && Map.canReach(elkoy)) {
				sleep(randomNum(432,740));
				elkoy.interact("Follow");
				randomNum(600, 800);
				sleepUntil(() -> elkoyto2809digspot.contains(getLocalPlayer()), randomNum(6700, 8000));
			}
		}
		
		if (elkoyto2809digspot.contains(getLocalPlayer()) && !digspot2809.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9)) && !treegnomevillagethrugate.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(digspot2809.getCenter());
			sleep(randomNum(432,740));

		}
		
		if (digspot2809.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2827() {
		currentClue = 2827;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !draynorvillagefishing.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(260, 400));
			Dialogues.chooseOption(3);
			sleepUntil(() -> draynorvillagetp.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(120, 300));
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && !C2827tile.contains(getLocalPlayer()) && draynorvillagefishing.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(C2827tile.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (C2827tile.contains(getLocalPlayer()) && Inventory.contains("Spade") && draynorvillagefishing.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(5, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2831() {
		currentClue = 2831;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!Inventory.contains("Key (medium)") && !toweroflifetoardougnemonastery.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!Inventory.contains("Key (medium)") && !toweroflifetoardougnemonastery.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.TOWER_OF_LIFE);
					sleepUntil(() -> toweroflifetoardougnemonastery.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}

		if (!Inventory.contains("Key (medium)") && !ardougnemonasterybig.contains(getLocalPlayer()) && toweroflifetoardougnemonastery.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ardougnemonasterysmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (!Inventory.contains("Key (medium)") && ardougnemonasterybig.contains(getLocalPlayer()) && toweroflifetoardougnemonastery.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer())) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC monk = NPCs.closest(f -> f != null && f.getName().contentEquals("Monk")); 
					if (monk != null && Map.canReach(monk)) {
						monk.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (Inventory.contains("Key (medium)") && !varrockcentertochurch.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			
			Inventory.interact("Varrock teleport", "Break");
			sleepUntil(() -> varrockcentre.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(100,400));
		}
		
		if (Inventory.contains("Key (medium)") && varrockcentertochurch.contains(getLocalPlayer()) && !varrockchurchtoprightroom.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (getLocalPlayer().getY() >=  3443) {
				Walking.walk(varrockchurchtoprightroom.getRandomTile());
				sleep(randomNum(200, 400));
			} else if (getLocalPlayer().getY() < 3443) {
				Walking.walk(new Tile(randomNum(3245, 3246), randomNum(3449, 3450)));
				sleep(randomNum(200, 400));
			}
			
		}

		if (Inventory.contains("Key (medium)") && varrockcentertochurch.contains(getLocalPlayer()) && varrockchurchtoprightroom.contains(getLocalPlayer())) {
			GameObject chest = GameObjects.closest(f -> f.getName().contentEquals("Closed chest") && closechestvarrockchurh.contains(f));
			if (chest != null) {
				chest.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2835() {
		currentClue = 2835;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!Inventory.contains("Key (medium)") && !jericoshouseupstairs.contains(getLocalPlayer()) && !ardougnetptojericoshouse.contains(getLocalPlayer())) {
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!jericoshouseupstairs.contains(getLocalPlayer()) && !Inventory.contains("Key (medium)") && ardougnemarket.contains(getLocalPlayer()) && Equipment.contains("Magic shortbow") && Equipment.contains("Rune arrow")) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC guard = NPCs.closest(f -> f != null && f.getName().contentEquals("Guard")); 
					if (guard != null && Map.canReach(guard)) {
						guard.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (!jericoshouseupstairs.contains(getLocalPlayer()) && Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(6,9)) && !jericoshouse.contains(getLocalPlayer()) && ardougnetptojericoshouse.contains(getLocalPlayer())) {
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(jericoshousesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!jericoshouseupstairs.contains(getLocalPlayer()) && Inventory.contains("Key (medium)") && jericoshouse.contains(getLocalPlayer())) {
			GameObject ladder = GameObjects.closest(f -> f.getName().contentEquals("Ladder") && jericoshouse.contains(f) && f.hasAction("Climb-up"));
			if (ladder != null) {
				ladder.interact("Climb-up");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (jericoshouseupstairs.contains(getLocalPlayer()) && Inventory.contains("Key (medium)")) {
			GameObject drawers = GameObjects.closest(f -> f.getName().contentEquals("Drawers") && drawerspotupstairsjericoshouse.contains(f));
			if (drawers != null) {
				drawers.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 20) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2853() {
		currentClue = 2853;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto2853.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto2853.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("2");
				sleepUntil(() -> treegnomestrongholdspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && !gnomeballfield.contains(getLocalPlayer()) && treegnomestrongholdto2853.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (getLocalPlayer().getX() >= 2413) {
				Walking.walk(new Tile(2409, 3471));
				sleep(randomNum(200, 400));
			} else if (getLocalPlayer().getX() < 2413) {
				Walking.walk(refereenpcarea.getRandomTile());
				sleep(randomNum(200, 400));
			}			
		}
		
		if (gnomeballfield.contains(getLocalPlayer())) {
			NPC referee = NPCs.closest("Gnome ball referee"); 
			if (referee != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					referee.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("5096");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					while (!Equipment.contains("Magic shortbow")) {
						int i = 0;
						while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
							i++;
							sleep(randomNum(180,220));
						}
						sleep(randomNum(50, 80));
						while (gnomeballfield.contains(getLocalPlayer())) {
							if (Walking.shouldWalk(randomNum(6,9))) {
								Walking.walk(outsidegnomeballfield.getRandomTile());
								sleep(randomNum(200, 400));
							}
						}
						while (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow") && !gnomeballfield.contains(getLocalPlayer())) {
							Inventory.interact("Magic shortbow", "Wield");
							sleepUntil(() -> Equipment.contains("Magic shortbow"), randomNum(4300, 5500));
							sleep(randomNum(150, 300));
						}
					}
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					referee.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3582() {
		currentClue = 3582;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!CKStoslayertower.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!CKStoslayertower.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.CANIFIS);
					sleepUntil(() -> CKSteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}

		if (CKStoslayertower.contains(getLocalPlayer()) && !slayertower2digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walkExact(slayertower2digspot.getCenter());
			sleep(randomNum(200, 400));
		}

		if (slayertower2digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3590() {
		currentClue = 3590;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !karamjaarea.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(360, 500));
			Dialogues.chooseOption(2);
			sleepUntil(() -> karamjateleport.contains(getLocalPlayer()), randomNum(1500,2000));
			sleep(randomNum(120, 300));
		}
		
		
		if (Walking.shouldWalk(randomNum(6,9)) && !C3590tile.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(C3590tile.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (C3590tile.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,210));
			}
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3598() {
		currentClue = 3598;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!mcgruborswood.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!mcgruborswood.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.MCGRUBORS_WOOD);
					sleepUntil(() -> mcgruborswood.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (mcgruborswood.contains(getLocalPlayer()) && !mcgruborswoodcrateareabig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(mcgruborswoodcrateareasmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		
		if (mcgruborswoodcrateareabig.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate"));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3601() {
		currentClue = 3601;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !battlefieldofkhazadto3601.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (grandexchangearea.contains(getLocalPlayer()) && !battlefieldofkhazadto3601.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(7032,1040));
				Keyboard.type("3");
				sleepUntil(() -> battlefieldofkhazadteleport.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}
		
		if (getLocalPlayer().getZ() == 0 && Walking.shouldWalk(randomNum(6,9)) && battlefieldofkhazadto3601.contains(getLocalPlayer()) && !crate3601big.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(crate3601small.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (crate3601big.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate") && crateobject3601.contains(f));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3605() {
		currentClue = 3605;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
				sleep(randomNum(100,300));
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!southeasthousebrimhavenupstairs.contains(getLocalPlayer()) && !Inventory.contains("Key (medium)") && Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !karamjaarea.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(360, 500));
			Dialogues.chooseOption(2);
			sleepUntil(() -> karamjateleport.contains(getLocalPlayer()), randomNum(1500,2000));
			sleep(randomNum(120, 300));
		}
		
		if (!southeasthousebrimhavenupstairs.contains(getLocalPlayer()) && !Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(6,9)) && !brimhavencentral.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(WEBNODEBRIMHAVENCENTER.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!southeasthousebrimhavenupstairs.contains(getLocalPlayer()) && !Inventory.contains("Key (medium)") && brimhavencentral.contains(getLocalPlayer()) && Equipment.contains("Magic shortbow") && Equipment.contains("Rune arrow")) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC pirate = NPCs.closest(f -> f != null && f.getName().contentEquals("Pirate")); 
					if (pirate != null && Map.canReach(pirate)) {
						pirate.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (!southeasthousebrimhavenupstairs.contains(getLocalPlayer()) && Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(6,9)) && !southeasthousebrimhaven.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(southeasthousebrimhaven.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!southeasthousebrimhavenupstairs.contains(getLocalPlayer()) && Inventory.contains("Key (medium)") && southeasthousebrimhaven.contains(getLocalPlayer())) {
			GameObject ladder = GameObjects.closest(f -> f.getName().contentEquals("Ladder") && southeasthousebrimhaven.contains(f) && f.hasAction("Climb-up"));
			if (ladder != null) {
				ladder.interact("Climb-up");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(4300, 5500));
				sleep(randomNum(700, 900));
			}
		}
		
		if (southeasthousebrimhavenupstairs.contains(getLocalPlayer()) && Inventory.contains("Key (medium)")) {
			GameObject drawers = GameObjects.closest(f -> f.getName().contentEquals("Drawers"));
			if (drawers != null) {
				drawers.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 20) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3609() {
		currentClue = 3609;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!CKStocanifis.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!CKStocanifis.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.CANIFIS);
					sleepUntil(() -> CKSteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && CKStocanifis.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && !canifisclothesshop.contains(getLocalPlayer())) {
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(canifisclothesshopsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (canifisclothesshop.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate") && crate3609area.contains(f));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3613() {
		currentClue = 3613;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !burthorpetosabacave.contains(getLocalPlayer()) && !sabacave.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> burthorpeteleport.contains(getLocalPlayer()), randomNum(3500,5000));
			sleep(randomNum(520, 800));
		}

		if (Walking.shouldWalk(randomNum(4,6)) && burthorpetosabacave.contains(getLocalPlayer()) && !sabacave.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(sabacaveentrance.getRandomTile());
			sleep(randomNum(200, 400));
			
			GameObject sabacaveentranceobj = GameObjects.closest("Cave Entrance");
			if (sabacaveentranceobj != null && sabacaveentranceobj.getTile().distance() <= 14) {
				sabacaveentranceobj.interact("Enter");
				sleepUntil(() -> sabacave.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}

		if (!burthorpetosabacave.contains(getLocalPlayer()) && sabacave.contains(getLocalPlayer())) {
			NPC saba = NPCs.closest("Saba"); 
			if (saba != null) {
				saba.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3615() {
		currentClue = 3615;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!CKStocanifis.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!CKStocanifis.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.CANIFIS);
					sleepUntil(() -> CKSteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && CKStocanifis.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && !canifistavern.contains(getLocalPlayer())) {
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(canifistavernsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (canifistavern.contains(getLocalPlayer())) {
			NPC roavar = NPCs.closest("Roavar"); 
			if (roavar != null) {
				roavar.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7274() {
		currentClue = 7274;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!keldagrimtorelleka.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!keldagrimtorelleka.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.KELDAGRIM_ENTRANCE);
					sleepUntil(() -> keldagrimtorelleka.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && keldagrimtorelleka.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && !rellekamainhall.contains(getLocalPlayer())) {
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(webnoderellekamainhall.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (keldagrimtorelleka.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && rellekamainhall.contains(getLocalPlayer())) {
			NPC brundt = NPCs.closest("Brundt the Chieftain"); 
			if (brundt != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					brundt.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("4");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					brundt.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7296() {
		currentClue = 7296;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!Inventory.contains("Key (medium)") && Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !edgevilletobarbvillage.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> edgevillecentre.contains(getLocalPlayer()), randomNum(4500,8000));
			sleep(randomNum(320, 600));
		}
		
		if (edgevilletobarbvillage.contains(getLocalPlayer()) && !Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(6,9)) && !barbarianvillage.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(barbarianvillagewebnode.getCenter());
			sleep(randomNum(200, 400));
		}

		if (!Inventory.contains("Key (medium)") && barbarianvillage.contains(getLocalPlayer()) && Equipment.contains("Magic shortbow") && Equipment.contains("Rune arrow")) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC enemynpc = NPCs.closest(f -> f != null && f.getName().contentEquals("Barbarian")); 
					if (enemynpc != null && Map.canReach(enemynpc)) {
						enemynpc.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}

		if (Inventory.contains("Key (medium)") && !digsitetoexamcentre.contains(getLocalPlayer())) {
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Inventory.contains("Digsite teleport")) {
				Inventory.interact("Digsite teleport", "Teleport");
				sleepUntil(() -> digsiteteleport.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}

		if (Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(6,9)) && digsitetoexamcentre.contains(getLocalPlayer()) && !chestroomexamcentre.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(chestroomexamcentre.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (Inventory.contains("Key (medium)") && chestroomexamcentre.contains(getLocalPlayer())) {
			GameObject chest = GameObjects.closest(f -> chesttile7296.contains(f) && f.getName().contentEquals("Closed chest"));
			if (chest != null) {
				chest.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 20) {
					i++;
					sleep(randomNum(180,250));
				}
				sleep(randomNum(10, 35));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7298() {
		currentClue = 7298;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (insidelighthouseupstairs.contains(getLocalPlayer())) {
			GameObject drawer = GameObjects.closest(f -> f.getName().contentEquals("Drawers") && drawers7298.contains(f));
			if (drawer != null) {
				drawer.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(10,30));
				if (Combat.isAutoRetaliateOn() == true) {
					sleep(randomNum(100,300));
		            Combat.toggleAutoRetaliate(false);
		            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
					sleep(randomNum(100,300));
				}
			}
		}
		
		if (!insidelighthouseupstairs.contains(getLocalPlayer()) && !Inventory.contains("Key (medium)") && !keldagrimtorelleka.contains(getLocalPlayer()) && !insidelighthouse.contains(getLocalPlayer()) && !lighthousebig.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!Inventory.contains("Key (medium)") && !keldagrimtorelleka.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.KELDAGRIM_ENTRANCE);
					sleepUntil(() -> keldagrimtorelleka.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (!Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(6,9)) && keldagrimtorelleka.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && !rellekamarket.contains(getLocalPlayer())) {
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(true);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(webnoderellekamarket.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Key (medium)") && Equipment.contains("Magic shortbow") && Equipment.contains("Rune arrow") && keldagrimtorelleka.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && rellekamarket.contains(getLocalPlayer())) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC enemynpc = NPCs.closest(f -> f != null && f.getName().contentEquals("Market Guard")); 
					if (enemynpc != null && Map.canReach(enemynpc)) {
						enemynpc.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (getLocalPlayer().getZ() == 0 && Inventory.contains("Key (medium)") && !insidelighthouse.contains(getLocalPlayer()) && !lighthousebig.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer())) {
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (Inventory.contains("Key (medium)") && !insidelighthouse.contains(getLocalPlayer()) && !lighthousebig.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring) && fairyring.distance() <= 6) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.LIGHTHOUSE);
					sleepUntil(() -> lighthousebig.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow") && lighthousebig.contains(getLocalPlayer())) {
					Inventory.interact("Magic shortbow", "Wield");
					sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (Inventory.contains("Key (medium)") && !insidelighthouse.contains(getLocalPlayer()) && lighthousebig.contains(getLocalPlayer()) && !insidelighthouseupstairs.contains(getLocalPlayer())) {
			GameObject doorway = GameObjects.closest("Doorway");
			 if (doorway != null) {
				doorway.interact("Walk-through");
				sleepUntil(() -> insidelighthouse.contains(getLocalPlayer()), randomNum(4500, 5600));
				sleep(randomNum(1200,1800));
			}
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow") && lighthousebig.contains(getLocalPlayer())) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (Inventory.contains("Key (medium)") && insidelighthouse.contains(getLocalPlayer()) && !lighthousebig.contains(getLocalPlayer()) && !insidelighthouseupstairs.contains(getLocalPlayer())) {
			GameObject stairs = GameObjects.closest("Staircase");
			if (stairs != null) {
				stairs.interact("Climb-up");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(4500, 5600));
				sleep(randomNum(700,900));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2801() {
		currentClue = 2801;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !draynorvillagetoham.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(3);
			sleepUntil(() -> draynorvillagetp.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(120, 300));
		}
		
		if (draynorvillagetoham.contains(getLocalPlayer()) && !C2801digtile.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4, 6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(C2801digtile.getCenter());
			sleep(randomNum(200, 400));
		}

		if (C2801digtile.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3614() {
		currentClue = 3614;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!CKStoslayertower.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!CKStomortmyrefungusgate.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.CANIFIS);
					sleepUntil(() -> CKSteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (CKStomortmyrefungusgate.contains(getLocalPlayer()) && !mortmyrefungusgate.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(mortmyrefungusgatesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (mortmyrefungusgate.contains(getLocalPlayer())) {
			NPC ulizius = NPCs.closest("Ulizius"); 
			if (ulizius != null) {
				ulizius.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19758() {
		currentClue = 19758;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!arceuustosoulaltar.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!arceuustosoulaltar.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.ARCEUUS_LIBRARY);
					sleepUntil(() -> arceuuslibraryteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (arceuustosoulaltar.contains(getLocalPlayer()) && !soulaltar.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(soulaltarsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
	
		if (soulaltar.contains(getLocalPlayer())) {
			NPC aretha = NPCs.closest("Aretha"); 
			if (aretha != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					aretha.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("2");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					aretha.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7304() {
		currentClue = 7304;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!rangingguild.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Combat bracelet("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.HANDS, "Ranging Guild");
			sleepUntil(() -> rangingguild.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (rangingguild.contains(getLocalPlayer()) && !rangingguildboxarea.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {

			GameObject guilddoor = GameObjects.closest("Guild door");
			if (guilddoor != null && guilddoor.getTile().distance() <= 20) {
				guilddoor.interact("Open");
				sleepUntil(() -> rangingguildgotthroughdoor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(rangingguildboxareasmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (rangingguildboxarea.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate") && rangingguildbox.contains(f));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19766() {
		currentClue = 19766;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			teleported = 0;
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (teleported == 0 && !portpascariliusbig.contains(getLocalPlayer()) && !cptkhaledhouse.contains(getLocalPlayer()) && Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !draynorvillagetoportsarim.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			teleported = 1;
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(3);
			sleepUntil(() -> draynorvillagetp.contains(getLocalPlayer()), randomNum(4500,6000));
			if (!draynorvillagetp.contains(getLocalPlayer())) {
				teleported = 0;
			}
			sleep(randomNum(120, 300));
		}
		
		if (!portpascariliusbig.contains(getLocalPlayer()) && !cptkhaledhouse.contains(getLocalPlayer()) && draynorvillagetoportsarim.contains(getLocalPlayer()) && !veosbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(veosnode.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!portpascariliusbig.contains(getLocalPlayer()) && !cptkhaledhouse.contains(getLocalPlayer()) && veosbig.contains(getLocalPlayer())) {
			NPC veos = NPCs.closest("Veos"); 
			if (veos != null) {
				veos.interact("Port Piscarilius");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(5500, 6400));
				sleep(randomNum(400, 700));
			}
		}
		
		
		if (getLocalPlayer().getZ() == 1) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				sleep(randomNum(2000, 3000));
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			if (getLocalPlayer().getZ() == 0) {
				teleported = 0;
			}
		}
		
		if (portpascariliusbig.contains(getLocalPlayer()) && !cptkhaledhouse.contains(getLocalPlayer()) && !draynorvillagetoportsarim.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(cptkhaledhouse.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (portpascariliusbig.contains(getLocalPlayer()) && cptkhaledhouse.contains(getLocalPlayer()) && !draynorvillagetoportsarim.contains(getLocalPlayer())) {
			NPC cptkhaled = NPCs.closest("Captain Khaled"); 
			if (cptkhaled != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					cptkhaled.interact("Talk-to");
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("5");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					cptkhaled.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}

		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3602() {
		currentClue = 3602;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && !rimmingtontochemistshouse.contains(getLocalPlayer()) && !rimmingtonnexttochemistshouse.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && !rimmingtontochemistshouse.contains(getLocalPlayer()) && !rimmingtonnexttochemistshouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ardougneportsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && ardougneport.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !rimmingtontochemistshouse.contains(getLocalPlayer()) && !rimmingtonnexttochemistshouse.contains(getLocalPlayer())) {
			NPC capbarnaby = NPCs.closest("Captain Barnaby"); 
			if (capbarnaby != null) {
				capbarnaby.interact("Rimmington");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(6500, 8500));
				sleep(randomNum(400,700));
			}
		}
		
		if (rimmingtonboats.contains(getLocalPlayer())) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && rimmingtontochemistshouse.contains(getLocalPlayer()) && !rimmingtonnexttochemistshouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(rimmingtonnexttochemistshouse.getCenter());
			sleep(randomNum(300, 500));
		}
		
		if (!rimmingtonboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && rimmingtontochemistshouse.contains(getLocalPlayer()) && rimmingtonnexttochemistshouse.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7284() {
		currentClue = 7284;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!varrocktovarrockcastle.contains(getLocalPlayer())) {
			if (Inventory.contains("Varrock teleport")) {
				Inventory.interact("Varrock teleport", "Break");
				sleepUntil(() -> varrockcentre.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (varrocktovarrockcastle.contains(getLocalPlayer()) && !varrockcastleroaldsroom.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(varrockcastleroaldsroomsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (varrockcastleroaldsroom.contains(getLocalPlayer())) {
			NPC roald = NPCs.closest("King Roald"); 
			if (roald != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					roald.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("24");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					roald.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
				
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12025() {
		currentClue = 12025;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!camelottocourthouse.contains(getLocalPlayer())) {
			if (Inventory.contains("Camelot teleport")) {
				Inventory.interact("Camelot teleport", "Break");
				sleepUntil(() -> camelotteleport.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!Inventory.contains("Diamond ring") && !Equipment.contains("Diamond ring") && camelottocourthouse.contains(getLocalPlayer()) && !infrontofcourthouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
	    	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(infrontofcourthousesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		  
		if (!Inventory.contains("Diamond ring") && !Equipment.contains("Diamond ring") && infrontofcourthouse.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Diamond ring") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int diamondring = 0;
		int halberd = 0;
		int mysticrobebottom = 0;
		
		if (Inventory.contains("Diamond ring") && !Equipment.contains("Diamond ring") && infrontofcourthouse.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			diamondring = Inventory.slot(f -> f.getName().contains("Diamond ring"));
			Inventory.slotInteract(diamondring, "Wear");
			sleepUntil(() -> Equipment.contains("Diamond ring"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			halberd = Inventory.slot(f -> f.getName().contains("Adamant halberd"));
			Inventory.slotInteract(halberd, "Wield");
			sleepUntil(() -> Equipment.contains("Adamant halberd"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			mysticrobebottom = Inventory.slot(f -> f.getName().contains("Mystic robe bottom"));
			Inventory.slotInteract(mysticrobebottom, "Wear");
			sleepUntil(() -> Equipment.contains("Mystic robe bottom"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Diamond ring") && Equipment.contains("Diamond ring") && camelottocourthouse.contains(getLocalPlayer()) && !insidecourthouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
	    	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(insidecourthousesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Diamond ring") && Equipment.contains("Diamond ring") && insidecourthouse.contains(getLocalPlayer())) {
			sleep(randomNum(60, 150));
			if(Tabs.getOpen() != Tab.EMOTES) {
				Tabs.open(Tab.EMOTES);
				sleep(randomNum(73,212));
			}
			
			int scrollx = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx+(scrollwidth/2)+randomNum(1,4), scrolly+(scrollheight/2)+randomNum(1,4)));
			sleep(randomNum(60, 150));

			Widgets.getWidget(216).getChild(1).getChild(20).interact("Clap");
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			
			sleep(randomNum(60, 150));
			int scrollx1 = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly1 = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight1 = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth1 = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx1+(scrollwidth1/2)+randomNum(1,4), scrolly1+(scrollheight1/8)+randomNum(1,4)));
			
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.SPIN);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Diamond ring") && Equipment.contains("Diamond ring")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Diamond ring") || Equipment.contains("Diamond ring")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					while (Equipment.contains("Mystic robe bottom")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						int ringofwealth = Inventory.slot(f -> f.getName().contains("Ring of wealth"));
						Inventory.slotInteract(ringofwealth, "Wear");
						sleepUntil(() -> !Equipment.contains("Diamond ring"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Adamant halberd"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int greendhidechaps = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
						Inventory.slotInteract(greendhidechaps, "Wear");
						sleepUntil(() -> !Equipment.contains("Mystic robe bottom"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
	
					while (Inventory.contains("Diamond ring") && !Equipment.contains("Diamond ring") && camelottocourthouse.contains(getLocalPlayer()) && !infrontofcourthouse.contains(getLocalPlayer())) {
							Walking.walk(infrontofcourthousesmall.getRandomTile());
							sleep(randomNum(500, 800));
							sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(4500, 6500));
							sleep(randomNum(100, 300));
						}
					
					while (Inventory.contains("Diamond ring") && !Equipment.contains("Diamond ring") && camelottocourthouse.contains(getLocalPlayer()) && infrontofcourthouse.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Maple longbow") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19734() {
		currentClue = 19734;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Skills necklace(")) && !woodcuttingguildtoshayzienring.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int skillsneck = Inventory.slot(f -> f.getName().contains("Skills necklace("));
			Inventory.slotInteract(skillsneck, "Rub");
			sleep(randomNum(645,800));
			Widgets.getWidget(187).getChild(3).getChild(4).interact("Continue");
			sleepUntil(() -> woodcuttingguildtp.contains(getLocalPlayer()), randomNum(3500,4800));
			sleep(randomNum(745,900));
		}

		if (woodcuttingguildtoshayzienring.contains(getLocalPlayer()) && !buildingeastofshayzienring.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (!getLocalPlayer().isMoving() && woodcuttingguildtp.contains(getLocalPlayer())) {
				Walking.walk(webnodebugfixareawoodcuttingguild.getRandomTile());
				sleep(randomNum(300, 500));
			} else {
				if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
					int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
					if (Tabs.getOpen() != Tab.INVENTORY) {
						Tabs.open(Tab.INVENTORY);
						sleep(randomNum(200,400));
					}
					Inventory.slotInteract(stampot, "Drink");
					sleep(randomNum(200,300));
				}
				
				Walking.walk(buildingeastofshayzienringsmall.getRandomTile());
				sleep(randomNum(300, 500));
			}	
		}
		
		if (buildingeastofshayzienring.contains(getLocalPlayer())) {
			NPC ginea = NPCs.closest("Captain Ginea"); 
			if (ginea != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					ginea.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(400, 700));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("113");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					ginea.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(350, 600));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2845() {
		currentClue = 2845;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!ardougneteleporttozookeeper.contains(getLocalPlayer()) && Inventory.contains("Ardougne teleport")) {
			Inventory.interact("Ardougne teleport", "Break");
			sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
			sleep(randomNum(430, 600));
		}
		
		if (ardougneteleporttozookeeper.contains(getLocalPlayer()) && !zookeeperarea.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(zookeeperareasmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (ardougneteleporttozookeeper.contains(getLocalPlayer()) && zookeeperarea.contains(getLocalPlayer())) {
			NPC zookeeper = NPCs.closest("Zoo keeper"); 
			if (zookeeper != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					zookeeper.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("40");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					zookeeper.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10264() {
		currentClue = 10264;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
	
		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !barbarianoutposttoagility.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int gamesneck = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(gamesneck, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(2);
			sleepUntil(() -> barbarianoutpostteleport.contains(getLocalPlayer()), randomNum(1500,2000));
			sleep(randomNum(520, 600));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && barbarianoutposttoagility.contains(getLocalPlayer()) && !barbianoutpostbeforeagilityroom.contains(getLocalPlayer()) && !barbianoutpostagilityroom.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(barbianoutpostbeforeagilityroompipe.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (barbarianoutposttoagility.contains(getLocalPlayer()) && barbianoutpostbeforeagilityroom.contains(getLocalPlayer()) && !barbianoutpostagilityroom.contains(getLocalPlayer())) {
			GameObject pipe = GameObjects.closest("Obstacle pipe");
			if (pipe != null) {
				pipe.interact("Squeeze-through");
				sleepUntil(() -> barbianoutpostagilityroom.contains(getLocalPlayer()), randomNum(7300, 8500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (!Inventory.contains("Maple shortbow") && barbarianoutposttoagility.contains(getLocalPlayer()) && !barbianoutpostbeforeagilityroom.contains(getLocalPlayer()) && barbianoutpostagilityroom.contains(getLocalPlayer())) {
			GameObject stash = GameObjects.closest("STASH (medium)");
			if (stash != null && stash.getTile().distance() <= 10) {
				stash.interact("Search");
				sleepUntil(() -> Inventory.contains("Maple shortbow"), randomNum(7300, 8500));
				sleep(randomNum(150, 300));
			} else if (((stash != null && stash.getTile().distance() > 10) || stash == null) && Walking.shouldWalk(randomNum(2,4))) {
				if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
					int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
					if (Tabs.getOpen() != Tab.INVENTORY) {
						Tabs.open(Tab.INVENTORY);
						sleep(randomNum(200,400));
					}
					Inventory.slotInteract(stampot, "Drink");
					sleep(randomNum(200,300));
				}
				
				Walking.walk(stashunitbarbarianoutpost.getRandomTile());
				sleep(randomNum(300, 500));
			}
		}
		
		int steelplatebody = 0;
		int mapleshortbow = 0;
		int teamcape = 0;
		
		if (Inventory.contains("Maple shortbow") && !Equipment.contains("Maple shortbow") && barbarianoutposttoagility.contains(getLocalPlayer()) && !barbianoutpostbeforeagilityroom.contains(getLocalPlayer()) && barbianoutpostagilityroom.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			steelplatebody = Inventory.slot(f -> f.getName().contains("Steel platebody"));
			Inventory.slotInteract(steelplatebody, "Wear");
			sleepUntil(() -> Equipment.contains("Steel platebody"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			mapleshortbow = Inventory.slot(f -> f.getName().contains("Maple shortbow"));
			Inventory.slotInteract(mapleshortbow, "Wield");
			sleepUntil(() -> Equipment.contains("Maple shortbow"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			teamcape = Inventory.slot(f -> f.getName().contains("Team-"));
			Inventory.slotInteract(teamcape, "Wear");
			sleepUntil(() -> !Equipment.contains("Green cape"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Maple shortbow") && Equipment.contains("Maple shortbow") && barbarianoutposttoagility.contains(getLocalPlayer()) && !barbianoutpostbeforeagilityroom.contains(getLocalPlayer()) && barbianoutpostagilityroom.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.CHEER);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.HEADBANG);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Maple shortbow") && Equipment.contains("Maple shortbow")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Maple shortbow") || Equipment.contains("Maple shortbow")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
	
					while (!Equipment.contains("Green cape") && Inventory.contains("Green cape")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.slotInteract(steelplatebody, "Wear");
						sleepUntil(() -> !Equipment.contains("Steel platebody"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.slotInteract(mapleshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Maple shortbow"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Green cape", "Wear");
						sleepUntil(() -> Equipment.contains("Green cape"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Maple shortbow") && !Equipment.contains("Maple shortbow")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Maple shortbow") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3611() {
		currentClue = 3611;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!outposttotreegnomestrongholddoor.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "The Outpost");
			sleepUntil(() -> theoutpostteleport.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && outposttotreegnomestrongholddoor.contains(getLocalPlayer()) && !treegnomestrongholddoor.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(treegnomestrongholddoorsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (treegnomestrongholddoor.contains(getLocalPlayer())) {
		NPC femi = NPCs.closest("Femi"); 
		if (femi != null) {
			femi.interact("Talk-to");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
			sleep(randomNum(200, 400));
			Dialogues.continueDialogue();
			sleepUntil(() -> Dialogues.canContinue(), randomNum(5500, 6400));
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}
			sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12033() {
		currentClue = 12033;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!akstofeldiphills.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!akstofeldiphills.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.FELDIP_HILLS_HUNTER);
					sleepUntil(() -> feldiphillsteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (akstofeldiphills.contains(getLocalPlayer()) && !C12033digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(C12033digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (akstofeldiphills.contains(getLocalPlayer()) && C12033digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2803() {
		currentClue = 2803;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!clstopeninsula.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!clstopeninsula.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.EAST_YANILLE);
					sleepUntil(() -> eastyanilleteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (clstopeninsula.contains(getLocalPlayer()) && !C2803digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(C2803digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (clstopeninsula.contains(getLocalPlayer()) && C2803digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19742() {
		currentClue = 19742;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!arceuustolibrary.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!arceuustolibrary.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.ARCEUUS_LIBRARY);
					sleepUntil(() -> arceuuslibraryteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (arceuustolibrary.contains(getLocalPlayer()) && !arceuuslibrary.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(arceuuslibrarysmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
	
		if (arceuuslibrary.contains(getLocalPlayer())) {
			NPC horphis = NPCs.closest("Horphis"); 
			if (horphis != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					horphis.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("1");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					horphis.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12029() {
		currentClue = 12029;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!blptotzhaarswordshop.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!blptotzhaarswordshop.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.TZHAAR);
					sleepUntil(() -> blpteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}

		if (blptotzhaarswordshop.contains(getLocalPlayer()) && !tzhaarswordshop.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(tzhaarswordshopsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Mystic gloves") && !Equipment.contains("Mystic gloves") && tzhaarswordshop.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Mystic gloves") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int steellongsword = 0;
		int bluedhidebody = 0;
		int mysticgloves = 0;
		
		if (Inventory.contains("Mystic gloves") && !Equipment.contains("Mystic gloves") && tzhaarswordshop.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			steellongsword = Inventory.slot(f -> f.getName().contains("Steel longsword"));
			Inventory.slotInteract(steellongsword, "Wield");
			sleepUntil(() -> Equipment.contains("Steel longsword"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			bluedhidebody = Inventory.slot(f -> f.getName().contains("Blue d'hide body"));
			Inventory.slotInteract(bluedhidebody, "Wear");
			sleepUntil(() -> Equipment.contains("Blue d'hide body"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			mysticgloves = Inventory.slot(f -> f.getName().contains("Mystic gloves"));
			Inventory.slotInteract(mysticgloves, "Wear");
			sleepUntil(() -> Equipment.contains("Mystic gloves"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Mystic gloves") && Equipment.contains("Mystic gloves") && tzhaarswordshop.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.JUMP_FOR_JOY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.SHRUG);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Mystic gloves") && Equipment.contains("Mystic gloves")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Mystic gloves") || Equipment.contains("Mystic gloves")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Mystic gloves")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Steel longsword"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int greendhidebody = Inventory.slot(f -> f.getName().contains("Green d'hide body"));
						Inventory.slotInteract(greendhidebody, "Wear");
						sleepUntil(() -> !Equipment.contains("Blue d'hide body"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int combatbracelet = Inventory.slot(f -> f.getName().contains("Combat bracelet"));
						Inventory.slotInteract(combatbracelet, "Wear");
						sleepUntil(() -> !Equipment.contains("Mystic gloves"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Mystic gloves") && !Equipment.contains("Mystic gloves")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Mystic gloves") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7288() {
		currentClue = 7288;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!mortmyretoc7288digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!mortmyretoc7288digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.MORT_MYRE_SWAMP);
					sleepUntil(() -> mortmyreteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (mortmyretoc7288digspot.contains(getLocalPlayer()) && !c7288digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c7288digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (mortmyretoc7288digspot.contains(getLocalPlayer()) && c7288digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2848() {
		currentClue = 2848;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!brimhavenboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && !brimhavenporttohajedy.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6)) && !brimhavenporttohajedy.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ardougneportsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && ardougneport.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !brimhavenporttohajedy.contains(getLocalPlayer())) {
			NPC capbarnaby = NPCs.closest("Captain Barnaby"); 
			if (capbarnaby != null) {
				capbarnaby.interact("Brimhaven");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(6500, 8500));
				sleep(randomNum(400,700));
			}
		}
		
		if (brimhavenboats.contains(getLocalPlayer())) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
				
		if (!brimhavenboats.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6)) && !brimhavenhajedy.contains(getLocalPlayer()) && brimhavenporttohajedy.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(brimhavenhajedysmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && brimhavenhajedy.contains(getLocalPlayer()) && brimhavenporttohajedy.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer())) {
			NPC hajedy = NPCs.closest("Hajedy"); 
			if (hajedy != null) {
				hajedy.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2855() {
		currentClue = 2855;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!cjrtosinclairmansion.contains(getLocalPlayer()) && !sinclaurmansionupstairsdebug.contains(getLocalPlayer()) && !sinclairmansionupstairs.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (!cjrtosinclairmansion.contains(getLocalPlayer()) && !sinclairmansionupstairs.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SINCLAIR_MANSION);
					sleepUntil(() -> cjrteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (cjrtosinclairmansion.contains(getLocalPlayer()) && !cinclairmansionstairs.contains(getLocalPlayer()) && !sinclairmansionupstairs.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}

			GameObject largedoor = GameObjects.closest("Large door");
			if (largedoor != null && largedoor.getTile().distance() <= 10) {
				largedoor.interact("Open");
				sleepUntil(() -> afterlargedoorsinclairmansion.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(cinclairmansionstairssmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (cinclairmansionstairs.contains(getLocalPlayer()) && getLocalPlayer().getZ() < 1) {
			GameObject stairs = GameObjects.closest(f -> f.getName().contentEquals("Staircase") && f.hasAction("Climb-up"));
			if (stairs != null) {
				int currentfloor = getLocalPlayer().getZ();
				stairs.interact("Climb-up");
				sleepUntil(() -> getLocalPlayer().getZ() != currentfloor, randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (sinclairmansionupstairs.contains(getLocalPlayer()) && getLocalPlayer().getZ() == 1) {
			NPC donovan = NPCs.closest("Donovan the Family Handyman");
			if (donovan != null) {
				if (Map.canReach(donovan)) {
					donovan.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Map.canReach(donovan) && Walking.shouldWalk(randomNum(2,4))) {
					Walking.walk(donovan.getTile());
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2843() {
		currentClue = 2843;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!lumbridgegraveyardtocastle.contains(getLocalPlayer())) {
			if (Inventory.contains("Lumbridge graveyard teleport")) {
				Inventory.interact("Lumbridge graveyard teleport", "Break");
				sleepUntil(() -> lumbridgegraveyardtp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (lumbridgegraveyardtocastle.contains(getLocalPlayer()) && !lumbridgecastlecooksroom.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(lumbridgecastlecooksroomsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (lumbridgegraveyardtocastle.contains(getLocalPlayer()) && lumbridgecastlecooksroom.contains(getLocalPlayer())) {
			NPC cook = NPCs.closest("Cook"); 
			if (cook != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					cook.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("9");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					cook.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19768() {
		currentClue = 19768;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!pohtoalithekebab.contains(getLocalPlayer())) {
			if (Inventory.contains("Teleport to house")) {
				Inventory.interact("Teleport to house", "Break");
				sleepUntil(() -> pohpolniveachtp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
        }
		
		
		if (pohtoalithekebab.contains(getLocalPlayer()) && !alithekebab.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(alithekebabsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (alithekebab.contains(getLocalPlayer())) {
			NPC kebabseller = NPCs.closest("Ali the Kebab seller"); 
			if (kebabseller != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					kebabseller.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("399");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					kebabseller.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12035() {
		currentClue = 12035;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!dlqtouzer.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!dlqtouzer.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.NORTH_NARDAH);
					sleepUntil(() -> dlqteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (dlqtouzer.contains(getLocalPlayer()) && !C12035digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,8))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (getLocalPlayer().getHealthPercent() <= 40 && Inventory.contains("Shark")) {
				Inventory.interact("Shark", "Eat");
				sleep(randomNum(200, 400));
			}
			
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (getLocalPlayer().getY() <= 3070) {
				Walking.walk(C12035inbetweenspot.getCenter());
				sleep(randomNum(200, 400));
			} else if (getLocalPlayer().getY() > 3070) {
				Walking.walk(C12035digspot.getCenter());
				sleep(randomNum(200, 400));
			}
		}
		
		if (dlqtouzer.contains(getLocalPlayer()) && C12035digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19736() {
		currentClue = 19736;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!clstonmz.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!clstonmz.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.EAST_YANILLE);
					sleepUntil(() -> eastyanilleteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (clstonmz.contains(getLocalPlayer()) && !nmzbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(nmzsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (nmzbig.contains(getLocalPlayer())) {
			NPC onion = NPCs.closest("Dominic Onion"); 
			if (onion != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					onion.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("9500");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					onion.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2847() {
		currentClue = 2847;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!varrockcentertoarchershop.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			
			Inventory.interact("Varrock teleport", "Break");
			sleepUntil(() -> varrockcentre.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(100,400));
		}
		
		if (varrockcentertoarchershop.contains(getLocalPlayer()) && !varrockarchershop.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(varrockarchershopsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (varrockarchershop.contains(getLocalPlayer())) {
			NPC lowe = NPCs.closest("Lowe"); 
			if (lowe != null) {
				lowe.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3592() {
		currentClue = 3592;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto12045.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto12045.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("2");
				sleepUntil(() -> treegnomestrongholdspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !digspot3592.contains(getLocalPlayer()) && treegnomestrongholdto12045.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (getLocalPlayer().getX() >= 2401) {
				Walking.walk(new Tile(randomNum(2397, 2389), randomNum(3423, 3424)));
				sleep(randomNum(200, 400));
			}
			if (getLocalPlayer().getX() < 2401) {
				Walking.walkExact(digspot3592.getCenter());
				sleep(randomNum(200, 400));
			}
		}
		
		if (digspot3592.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3616() {
		currentClue = 3616;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!duelarenatohospital.contains(getLocalPlayer())) {
			int ringofduelingslot = Inventory.slot(f -> f.getName().contains("Ring of dueling"));
			if (Inventory.contains(f -> f.getName().contains("Ring of dueling"))) {
				if(Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(73,212));
				}
				Inventory.slotInteract(ringofduelingslot, "Rub");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.chooseOption(1);
				sleepUntil(() -> duelarenateleport.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(130, 300));
			}
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && !duelarenahospitalbig.contains(getLocalPlayer()) && duelarenatohospital.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(duelarenahospitalsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (duelarenahospitalbig.contains(getLocalPlayer())) {
			NPC jaraah = NPCs.closest("Jaraah"); 
			if (jaraah != null) {
				jaraah.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(11500, 13400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12061() {
		currentClue = 12061;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			teleported = 0;
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (teleported == 0 && Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !draynorvillagetoportsarim.contains(getLocalPlayer())) {
			teleported = 1;
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(3);
			sleepUntil(() -> draynorvillagetp.contains(getLocalPlayer()), randomNum(4500,6000));
			if (!draynorvillagetp.contains(getLocalPlayer())) {
				teleported = 0;
			}
			sleep(randomNum(120, 300));
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && !portsarimtobiasbig.contains(getLocalPlayer()) && draynorvillagetoportsarim.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(portsarimtobiassmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (portsarimtobiasbig.contains(getLocalPlayer())) {
			teleported = 0;
			NPC captobi = NPCs.closest("Captain Tobias"); 
			if (captobi != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					captobi.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("6");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					captobi.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2837() {
		currentClue = 2837;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!rangingguildtoseersvillage.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Combat bracelet("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.HANDS, "Ranging Guild");
			sleepUntil(() -> outsiderangingguild.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (outsiderangingguild.contains(getLocalPlayer()) && !Inventory.contains("Key (medium)")) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC chicken = NPCs.closest(f -> f != null && f.getName().contentEquals("Chicken")); 
					if (chicken != null && Map.canReach(chicken)) {
						chicken.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (Inventory.contains("Key (medium)") && rangingguildtoseersvillage.contains(getLocalPlayer()) && !elementalworkshophouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(elementalworkshophousesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (elementalworkshophouse.contains(getLocalPlayer()) && Inventory.contains("Key (medium)")) {
			GameObject drawers = GameObjects.closest(f -> f.getName().contentEquals("Drawers") && drawerselementalworkshop.contains(f));
			if (drawers != null) {
				drawers.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 20) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23046() {
		currentClue = 23046;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!cirtomountkaruulm.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!cirtomountkaruulm.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(100, 250));
					FairyRings.travel(FairyLocation.KEBOS_LOWLANDS);
					sleepUntil(() -> cirteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}

		if (cirtomountkaruulm.contains(getLocalPlayer()) && !mountkaruulstash.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(mountkaruulstashsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Mithril boots") && !Equipment.contains("Mithril boots") && mountkaruulstash.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Mithril boots") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int adamantwarhammer = 0;
		int ringoflife = 0;
		int mithrilboots = 0;
		
		if (Inventory.contains("Mithril boots") && !Equipment.contains("Mithril boots") && mountkaruulstash.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			adamantwarhammer = Inventory.slot(f -> f.getName().contains("Adamant warhammer"));
			Inventory.slotInteract(adamantwarhammer, "Wield");
			sleepUntil(() -> Equipment.contains("Adamant warhammer"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			ringoflife = Inventory.slot(f -> f.getName().contains("Ring of life"));
			Inventory.slotInteract(ringoflife, "Wear");
			sleepUntil(() -> Equipment.contains("Ring of life"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			mithrilboots = Inventory.slot(f -> f.getName().contains("Mithril boots"));
			Inventory.slotInteract(mithrilboots, "Wear");
			sleepUntil(() -> Equipment.contains("Mithril boots"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Mithril boots") && Equipment.contains("Mithril boots") && mountkaruulstash.contains(getLocalPlayer())) {			
			
			sleep(randomNum(60, 150));
			if(Tabs.getOpen() != Tab.EMOTES) {
				Tabs.open(Tab.EMOTES);
				sleep(randomNum(73,212));
			}
			
			int scrollx = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx+(scrollwidth/2)+randomNum(1,4), scrolly+(scrollheight/2)+randomNum(1,4)));
			sleep(randomNum(60, 150));

			Widgets.getWidget(216).getChild(1).getChild(20).interact("Clap");
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			int scrollx1 = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly1 = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight1 = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth1 = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx1+(scrollwidth1/2)+randomNum(1,4), scrolly1+(scrollheight1/8)+randomNum(1,4)));
			sleep(randomNum(60, 150));
			
			Emotes.doEmote(Emote.SPIN);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Mithril boots") && Equipment.contains("Mithril boots")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Mithril boots") || Equipment.contains("Mithril boots")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Mithril boots") && !Inventory.contains("Mithril boots")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Adamant warhammer"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int ringofwealth = Inventory.slot(f -> f.getName().contains("Ring of wealth"));
						Inventory.slotInteract(ringofwealth, "Wear");
						sleepUntil(() -> !Equipment.contains("Ring of life"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int bootsoflightness = Inventory.slot(f -> f.getName().contains("Boots of lightness"));
						Inventory.slotInteract(bootsoflightness, "Wear");
						sleepUntil(() -> !Equipment.contains("Mithril boots"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Mithril boots") && !Equipment.contains("Mithril boots")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Mithril boots") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7292() {
		currentClue = 7292;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!lighthouseto7292digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!lighthouseto7292digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.LIGHTHOUSE);
					sleepUntil(() -> lighthousebig.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (lighthouseto7292digspot.contains(getLocalPlayer()) && !c7292digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c7292digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (lighthouseto7292digspot.contains(getLocalPlayer()) && c7292digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7305() {
		currentClue = 7305;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!cipto7305digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!cipto7305digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.MISCELLANIA);
					sleepUntil(() -> cipteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (cipto7305digspot.contains(getLocalPlayer()) && !c7305digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(5,7))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walkExact(c7305digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (cipto7305digspot.contains(getLocalPlayer()) && c7305digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3588() {
		currentClue = 3588;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !karamjato3588digspot.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(2);
			sleepUntil(() -> karamjateleport.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(120, 300));
		}
		
		if (karamjato3588digspot.contains(getLocalPlayer()) && !c3588digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c3588digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (karamjato3588digspot.contains(getLocalPlayer()) && c3588digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2817() {
		currentClue = 2817;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!fairyrintoshilomine.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!fairyrintoshilomine.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SOUTH_TAI_BWO_WANNAI_VILLAGE);
					sleepUntil(() -> CKRteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (fairyrintoshilomine.contains(getLocalPlayer()) && !shilominedigspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(5,7))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walkExact(shilominedigspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (fairyrintoshilomine.contains(getLocalPlayer()) && shilominedigspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12039() {
		currentClue = 12039;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!keldagrimtorelleka.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!keldagrimtorelleka.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.KELDAGRIM_ENTRANCE);
					sleepUntil(() -> keldagrimtorelleka.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && keldagrimtorelleka.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && !c12039digspot.contains(getLocalPlayer())) {
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walkExact(c12039digspot.getCenter());
			sleep(randomNum(200, 400));
		}

		if (keldagrimtorelleka.contains(getLocalPlayer()) && c12039digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7276() {
		currentClue = 7276;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!fairyrintotaibwowannai.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!fairyrintotaibwowannai.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SOUTH_TAI_BWO_WANNAI_VILLAGE);
					sleepUntil(() -> CKRteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (fairyrintotaibwowannai.contains(getLocalPlayer()) && !taibwowannaifencemiddle.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(5,7))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(taibwowannaifencemiddlesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (taibwowannaifencemiddle.contains(getLocalPlayer())) {
			NPC gabooty = NPCs.closest("Gabooty"); 
			if (gabooty != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					gabooty.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("11");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					gabooty.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7307() {
		currentClue = 7307;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!akstofeldiphills2.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!akstofeldiphills2.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.FELDIP_HILLS_HUNTER);
					sleepUntil(() -> feldiphillsteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (akstofeldiphills2.contains(getLocalPlayer()) && !c7307digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c7307digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (akstofeldiphills2.contains(getLocalPlayer()) && c7307digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12021() {
		currentClue = 12021;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!lumbridgegraveyardtoswamp.contains(getLocalPlayer()) && !lumbridgeswampcave.contains(getLocalPlayer()) && Inventory.contains("Lumbridge graveyard teleport")) {
			Inventory.interact("Lumbridge graveyard teleport", "Break");
			sleepUntil(() -> lumbridgegraveyardtp.contains(getLocalPlayer()), randomNum(3000,4000));
			sleep(randomNum(200,400));
		}
		
		if (lumbridgegraveyardtoswamp.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(lumbyswampcaveentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));
			
			GameObject darkhole = GameObjects.closest("Dark hole");
			if (darkhole != null && darkhole.getTile().distance() <= 14) {
				darkhole.interact("Climb-down");
				sleepUntil(() -> lumbridgeswampcave.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (!Inventory.contains("Amulet of power") && !Equipment.contains("Amulet of power") && lumbridgeswampcave.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Amulet of power") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int staffofair = 0;
		int bronzefullhelm = 0;
		int amuletofpower = 0;
		
		if (Inventory.contains("Amulet of power") && !Equipment.contains("Amulet of power") && lumbridgeswampcave.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			staffofair = Inventory.slot(f -> f.getName().contains("Staff of air"));
			Inventory.slotInteract(staffofair, "Wield");
			sleepUntil(() -> Equipment.contains("Staff of air"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			bronzefullhelm = Inventory.slot(f -> f.getName().contains("Bronze full helm"));
			Inventory.slotInteract(bronzefullhelm, "Wear");
			sleepUntil(() -> Equipment.contains("Bronze full helm"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			amuletofpower = Inventory.slot(f -> f.getName().contains("Amulet of power"));
			Inventory.slotInteract(amuletofpower, "Wear");
			sleepUntil(() -> Equipment.contains("Amulet of power"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Amulet of power") && Equipment.contains("Amulet of power") && lumbridgeswampcave.contains(getLocalPlayer())) {			
			Emotes.doEmote(Emote.DANCE);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.BLOW_KISS);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Amulet of power") && Equipment.contains("Amulet of power")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Amulet of power") || Equipment.contains("Amulet of power")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Amulet of power") && !Inventory.contains("Amulet of power")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Staff of air"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int kandarinheadgear = Inventory.slot(f -> f.getName().contains("Kandarin headgear 1"));
						Inventory.slotInteract(kandarinheadgear, "Wear");
						sleepUntil(() -> !Equipment.contains("Bronze full helm"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int necklaceofpassage = Inventory.slot(f -> f.getName().contains("Necklace of passage"));
						Inventory.slotInteract(necklaceofpassage, "Wear");
						sleepUntil(() -> !Equipment.contains("Amulet of power"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Amulet of power") && !Equipment.contains("Amulet of power")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Amulet of power") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19738() {
		currentClue = 19738;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Skills necklace(")) && !woodcuttingguildtohosidiusvinery.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int skillsneck = Inventory.slot(f -> f.getName().contains("Skills necklace("));
			Inventory.slotInteract(skillsneck, "Rub");
			sleep(randomNum(645,800));
			Widgets.getWidget(187).getChild(3).getChild(4).interact("Continue");
			sleepUntil(() -> woodcuttingguildtp.contains(getLocalPlayer()), randomNum(3500,4800));
			sleep(randomNum(745,900));
		}
		
		if (woodcuttingguildtohosidiusvinery.contains(getLocalPlayer()) && !northvineryhosidius.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(northvineryhosidiussmall.getRandomTile());
			sleep(randomNum(300, 500));
		}

		if (northvineryhosidius.contains(getLocalPlayer())) {
			NPC gallow = NPCs.closest("Gallow"); 
			if (gallow != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					gallow.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("12");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					gallow.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2849() {
		currentClue = 2849;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !alkharidtokebabshop.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(4);
			sleepUntil(() -> alkharidtproom.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(120, 300));
		}
		
		if (alkharidtokebabshop.contains(getLocalPlayer()) && !alkharidkebabshop.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			GameObject largedoor = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && alkharidlargedoor.contains(f));
			if (largedoor != null && largedoor.getTile().distance() <= 7 && alkharidtproom.contains(getLocalPlayer())) {
				largedoor.interact("Open");
				sleepUntil(() -> !alkharidtproom.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(alkharidkebabshopsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (alkharidkebabshop.contains(getLocalPlayer())) {
			NPC karim = NPCs.closest("Karim"); 
			if (karim != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					karim.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("5");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					karim.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12027() {
		currentClue = 12027;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!catherbytocamelotbeach.contains(getLocalPlayer()) && Inventory.contains("Camelot teleport")) { //&& C12067teleportbugfix == 0
			Inventory.interact("Camelot teleport", "Break");
			sleepUntil(() -> camelotteleport.contains(getLocalPlayer()), randomNum(2500,3500));
			sleep(randomNum(330, 500));
			/*if (catherbytocamelot.contains(getLocalPlayer())) {
				C12067teleportbugfix = 1;
			}*/
		}
		
		if (!Inventory.contains("Mithril platebody") && !Equipment.contains("Mithril platebody") && catherbytocamelotbeach.contains(getLocalPlayer()) && !camelotbeachstash.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(camelotbeachstashsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Mithril platebody") && !Equipment.contains("Mithril platebody") && camelotbeachstash.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Mithril platebody") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int adamantsqshield = 0;
		int bonedagger = 0;
		
		if (Inventory.contains("Mithril platebody") && !Equipment.contains("Mithril platebody") && camelotbeachstash.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			adamantsqshield = Inventory.slot(f -> f.getName().contains("Adamant sq shield"));
			Inventory.slotInteract(adamantsqshield, "Wield");
			sleepUntil(() -> Equipment.contains("Adamant sq shield"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			bonedagger = Inventory.slot(f -> f.getName().contains("Bone dagger"));
			Inventory.slotInteract(bonedagger, "Wield");
			sleepUntil(() -> Equipment.contains("Bone dagger"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Mithril platebody", "Wear");
			sleepUntil(() -> Equipment.contains("Mithril platebody"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Mithril platebody") && Equipment.contains("Mithril platebody") && catherbytocamelotbeach.contains(getLocalPlayer()) && !camelotbeach.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(camelotbeachsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Mithril platebody") && Equipment.contains("Mithril platebody") && camelotbeach.contains(getLocalPlayer())) {			
			Emotes.doEmote(Emote.CRY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.LAUGH);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Mithril platebody") && Equipment.contains("Mithril platebody")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Mithril platebody") || Equipment.contains("Mithril platebody")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Mithril platebody") && !Inventory.contains("Mithril platebody")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Bone dagger"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int greendhidebody = Inventory.slot(f -> f.getName().contains("Green d'hide body"));
						Inventory.slotInteract(greendhidebody, "Wear");
						sleepUntil(() -> !Equipment.contains("Mithril platebody"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Mithril platebody") && !Equipment.contains("Mithril platebody") && !camelotbeachstash.contains(getLocalPlayer())) {
						if (Walking.shouldWalk(randomNum(4,6))) {
							if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
								int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
								if (Tabs.getOpen() != Tab.INVENTORY) {
									Tabs.open(Tab.INVENTORY);
									sleep(randomNum(200,400));
								}
								Inventory.slotInteract(stampot, "Drink");
								sleep(randomNum(200,300));
							}
							
							Walking.walk(camelotbeachstashsmall.getRandomTile());
							sleep(randomNum(200, 400));
						}
					}

					
					while (Inventory.contains("Mithril platebody") && !Equipment.contains("Mithril platebody") && camelotbeachstash.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Mithril platebody") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19750() {
		currentClue = 19750;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!varrocktosouthentrance.contains(getLocalPlayer())) {
			if (Inventory.contains("Varrock teleport")) {
				Inventory.interact("Varrock teleport", "Break");
				sleepUntil(() -> varrockcentre.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (varrocktosouthentrance.contains(getLocalPlayer()) && !varrocksouthentrance.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(varrocksouthentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (varrocksouthentrance.contains(getLocalPlayer())) {
			NPC tramp = NPCs.closest("Charlie the Tramp"); 
			if (tramp != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					tramp.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("0");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					tramp.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23131() {
		currentClue = 23131;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			teleported = 0;
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (teleported == 0 && !portpascariliusbigger.contains(getLocalPlayer()) && !nicholasbig.contains(getLocalPlayer()) && Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !draynorvillagetoportsarim.contains(getLocalPlayer())) {
			teleported = 1;
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(3);
			sleepUntil(() -> draynorvillagetp.contains(getLocalPlayer()), randomNum(4500,6000));
			if (!draynorvillagetp.contains(getLocalPlayer())) {
				teleported = 0;
			}
			sleep(randomNum(120, 300));
		}
		
		if (!portpascariliusbigger.contains(getLocalPlayer()) && !nicholasbig.contains(getLocalPlayer()) && draynorvillagetoportsarim.contains(getLocalPlayer()) && !veosbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(veosnode.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (!portpascariliusbigger.contains(getLocalPlayer()) && !nicholasbig.contains(getLocalPlayer()) && veosbig.contains(getLocalPlayer())) {
			NPC veos = NPCs.closest("Veos"); 
			if (veos != null) {
				veos.interact("Port Piscarilius");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(5500, 6400));
				sleep(randomNum(400, 700));
			}
		}
		
		
		if (getLocalPlayer().getZ() == 1) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				sleep(randomNum(2000, 3000));
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			if (getLocalPlayer().getZ() == 0) {
				teleported = 0;
			}
		}
		
		if (portpascariliusbigger.contains(getLocalPlayer()) && !nicholasbig.contains(getLocalPlayer()) && !draynorvillagetoportsarim.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(nicholassmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (portpascariliusbigger.contains(getLocalPlayer()) && nicholasbig.contains(getLocalPlayer()) && !draynorvillagetoportsarim.contains(getLocalPlayer())) {
			NPC nicholas = NPCs.closest("Nicholas"); 
			if (nicholas != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					nicholas.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("4");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					nicholas.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12069() {
		currentClue = 12069;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!camelottocamelotcastle.contains(getLocalPlayer()) && !camelotcastlecourtyard.contains(getLocalPlayer())) {
			if (Inventory.contains("Camelot teleport")) {
				Inventory.interact("Camelot teleport", "Break");
				sleepUntil(() -> camelotteleport.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (camelottocamelotcastle.contains(getLocalPlayer()) && !camelotcastlecourtyard.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(camelotcastlecourtyardsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (camelotcastlecourtyard.contains(getLocalPlayer())) {
			NPC sirkay = NPCs.closest("Sir Kay"); 
			if (sirkay != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					sirkay.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("6");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					sirkay.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10278() {
		currentClue = 10278;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !alkharidtoshantaypass.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(4);
			sleepUntil(() -> alkharidtproom.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(120, 300));
		}
		
		if (alkharidtoshantaypass.contains(getLocalPlayer()) && !shantaypass.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			GameObject largedoor = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && alkharidlargedoor.contains(f));
			if (largedoor != null && largedoor.getTile().distance() <= 7 && alkharidtproom.contains(getLocalPlayer())) {
				largedoor.interact("Open");
				sleepUntil(() -> !alkharidtproom.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(shantaypassstash.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Bruise blue snelm") && !Equipment.contains("Bruise blue snelm") && shantaypass.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Bruise blue snelm") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int staffofair = 0;
		int bronzesqshield = 0;
		int bruisebluesnelm = 0;
		
		if (Inventory.contains("Bruise blue snelm") && !Equipment.contains("Bruise blue snelm") && shantaypass.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			staffofair = Inventory.slot(f -> f.getName().contains("Staff of air"));
			Inventory.slotInteract(staffofair, "Wield");
			sleepUntil(() -> Equipment.contains("Staff of air"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			bronzesqshield = Inventory.slot(f -> f.getName().contains("Bronze sq shield"));
			Inventory.slotInteract(bronzesqshield, "Wield");
			sleepUntil(() -> Equipment.contains("Bronze sq shield"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			bruisebluesnelm = Inventory.slot(f -> f.getName().contains("Bruise blue snelm"));
			Inventory.slotInteract(bruisebluesnelm, "Wear");
			sleepUntil(() -> Equipment.contains("Bruise blue snelm"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Bruise blue snelm") && Equipment.contains("Bruise blue snelm") && shantaypass.contains(getLocalPlayer()) && !shantaypassmiddle.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(1,3))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(shantaypassmiddlesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Bruise blue snelm") && Equipment.contains("Bruise blue snelm") && shantaypassmiddle.contains(getLocalPlayer())) {			
			Emotes.doEmote(Emote.JIG);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.BOW);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Bruise blue snelm") && Equipment.contains("Bruise blue snelm")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Bruise blue snelm") || Equipment.contains("Bruise blue snelm")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Bruise blue snelm") && !Inventory.contains("Bruise blue snelm")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> !Equipment.contains("Bronze sq shield"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int kandarinheadgear = Inventory.slot(f -> f.getName().contains("Kandarin headgear 1"));
						Inventory.slotInteract(kandarinheadgear, "Wear");
						sleepUntil(() -> !Equipment.contains("Bruise blue snelm"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Bruise blue snelm") && !Equipment.contains("Bruise blue snelm")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Bruise blue snelm") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
				
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19754() {
		currentClue = 19754;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!faladortofaladorcastle.contains(getLocalPlayer())) {
			if (Inventory.contains("Falador teleport")) {
				Inventory.interact("Falador teleport", "Break");
				sleepUntil(() -> faladortp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && faladortofaladorcastle.contains(getLocalPlayer()) && !faladorcastle.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(faladorcastlesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (faladorcastle.contains(getLocalPlayer())) {
			NPC squire = NPCs.closest("Squire"); 
			if (squire != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					squire.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("654");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					squire.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7282() {
		currentClue = 7282;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Skills necklace(")) && !fishingguildtoedmond.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int skillsneck = Inventory.slot(f -> f.getName().contains("Skills necklace("));
			Inventory.slotInteract(skillsneck, "Rub");
			sleep(randomNum(645,800));
			Widgets.getWidget(187).getChild(3).getChild(0).interact("Continue");
			sleepUntil(() -> fishingguildtp.contains(getLocalPlayer()), randomNum(3500,4800));
			sleep(randomNum(745,900));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && fishingguildtoedmond.contains(getLocalPlayer()) && !edmondshouse.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(edmondshousesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (edmondshouse.contains(getLocalPlayer())) {
			NPC edmond = NPCs.closest("Edmond"); 
			if (edmond != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					edmond.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("3");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					edmond.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3604() {
		currentClue = 3604;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!fairyrintotaibwowannai.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!fairyrintotaibwowannai.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SOUTH_TAI_BWO_WANNAI_VILLAGE);
					sleepUntil(() -> CKRteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (fairyrintotaibwowannai.contains(getLocalPlayer()) && !taibwowannaibrokenjunglefence.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(5,7))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(taibwowannaibrokenjunglefencesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (taibwowannaibrokenjunglefence.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate") && c3604crate.contains(f));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2833() {
		currentClue = 2833;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!ardougnetoardougnepub.contains(getLocalPlayer()) && !ardougnepubupstairs.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!Inventory.contains("Key (medium)") && ardougnetoardougnepub.contains(getLocalPlayer()) && !handelmortmansion.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(handelmortmansionsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Key (medium)") && ardougnetoardougnepub.contains(getLocalPlayer()) && handelmortmansion.contains(getLocalPlayer())) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC guarddog = NPCs.closest(f -> f != null && f.getName().contentEquals("Guard dog")); 
					if (guarddog != null && Map.canReach(guarddog)) {
						guarddog.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && Inventory.contains("Key (medium)") && !ardougnepubupstairs.contains(getLocalPlayer()) && ardougnetoardougnepub.contains(getLocalPlayer()) && !ardougnepub.contains(getLocalPlayer())) {
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ardougnepubsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (Inventory.contains("Key (medium)") && ardougnepub.contains(getLocalPlayer()) && getLocalPlayer().getZ() == 0) {
			GameObject stairs = GameObjects.closest(f -> f.getName().contentEquals("Staircase") && f.hasAction("Climb-up"));
			if (stairs != null) {
				stairs.interact("Climb-up");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (Inventory.contains("Key (medium)") && ardougnepubupstairs.contains(getLocalPlayer())) {
			GameObject drawers = GameObjects.closest(f -> f.getName().contentEquals("Drawers") && drawersc2833.contains(f));
			if (drawers != null) {
				drawers.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 20) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2857() {
		currentClue = 2857;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomevillagenotthrugate.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomevillagenotthrugate.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(7032,1040));
				Keyboard.type("1");
				sleepUntil(() -> treegnomevillagenotthrugate.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}
				
		if (treegnomevillagenotthrugate.contains(getLocalPlayer())) {
			NPC bolren = NPCs.closest("King Bolren"); 
			if (bolren != null) {
				bolren.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7280() {
		currentClue = 7280;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!ardougnetowitchaven.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (ardougnetowitchaven.contains(getLocalPlayer()) && !witchavennorth.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(witchavennorthsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (witchavennorth.contains(getLocalPlayer())) {
			NPC caroline = NPCs.closest("Caroline"); 
			if (caroline != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					caroline.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("11");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					caroline.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3596() {
		currentClue = 3596;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Skills necklace(")) && !craftingguildtohobgobisland.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int skillsneck = Inventory.slot(f -> f.getName().contains("Skills necklace("));
			Inventory.slotInteract(skillsneck, "Rub");
			sleep(randomNum(645,800));
			Widgets.getWidget(187).getChild(3).getChild(2).interact("Continue");
			sleepUntil(() -> craftingguildtp.contains(getLocalPlayer()), randomNum(3500,4800));
			sleep(randomNum(745,900));
		}
		
		if (craftingguildtohobgobisland.contains(getLocalPlayer()) && !c3596digtile.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c3596digtile.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (craftingguildtohobgobisland.contains(getLocalPlayer()) && c3596digtile.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3617() {
		currentClue = 3617;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && !brimhavenporttoshrimpandparrot.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6)) && !brimhavenporttoshrimpandparrot.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ardougneportsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && ardougneport.contains(getLocalPlayer()) && ardougnetoboat.contains(getLocalPlayer()) && !brimhavenporttoshrimpandparrot.contains(getLocalPlayer())) {
			NPC capbarnaby = NPCs.closest("Captain Barnaby"); 
			if (capbarnaby != null) {
				capbarnaby.interact("Brimhaven");
				sleepUntil(() -> getLocalPlayer().getZ() == 1, randomNum(6500, 8500));
				sleep(randomNum(400,700));
			}
		}
		
		if (brimhavenboats.contains(getLocalPlayer())) {
			GameObject gangplank = GameObjects.closest("Gangplank");
			if (gangplank != null) {
				gangplank.interact("Cross");
				sleepUntil(() -> getLocalPlayer().getZ() == 0, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
				
		if (!brimhavenboats.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6)) && !brimhavenshrimpandparrot.contains(getLocalPlayer()) && brimhavenporttoshrimpandparrot.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(brimhavenshrimpandparrotsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!brimhavenboats.contains(getLocalPlayer()) && brimhavenshrimpandparrot.contains(getLocalPlayer()) && brimhavenporttoshrimpandparrot.contains(getLocalPlayer()) && !ardougnetoboat.contains(getLocalPlayer()) && !ardougneport.contains(getLocalPlayer())) {
			NPC kangaimau = NPCs.closest("Kangai Mau"); 
			if (kangaimau != null) {
				kangaimau.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2841() {
		currentClue = 2841;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!hazelmerehousez1.contains(getLocalPlayer()) && !clstopeninsula.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!clstopeninsula.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.EAST_YANILLE);
					sleepUntil(() -> eastyanilleteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (clstopeninsula.contains(getLocalPlayer()) && !hazelmerehousez0.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(hazelmerehousez0small.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (clstopeninsula.contains(getLocalPlayer()) && hazelmerehousez0.contains(getLocalPlayer()) && getLocalPlayer().getZ() == 0) {
			GameObject ladder = GameObjects.closest(f -> f.getName().contentEquals("Ladder"));
			if (ladder != null) {
				ladder.interact("Climb-up");
				sleepUntil(() -> hazelmerehousez1.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (hazelmerehousez1.contains(getLocalPlayer())) {
			NPC hazel = NPCs.closest("Hazelmere"); 
			if (hazel != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					hazel.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("6859");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					hazel.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3584() {
		currentClue = 3584;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!mortmyretoc7288digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (!mortmyretoc3584digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.MORT_MYRE_SWAMP);
					sleepUntil(() -> mortmyreteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (mortmyretoc3584digspot.contains(getLocalPlayer()) && !c3584digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c3584digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (mortmyretoc3584digspot.contains(getLocalPlayer()) && c3584digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12049() {
		currentClue = 12049;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!rangingguildtoc12049digspot.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Combat bracelet("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.HANDS, "Ranging Guild");
			sleepUntil(() -> rangingguild.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (rangingguildtoc12049digspot.contains(getLocalPlayer()) && !c12049digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			GameObject logbalance = GameObjects.closest(f -> f.getName().contains("Log balance") && logbalancec12049.contains(f));
			if (logbalance != null && logbalance.getTile().distance() <= 12 && getLocalPlayer().getX() > 2599) {
				logbalance.interact("Walk-across");
				sleepUntil(() -> getLocalPlayer().getX() < 2599, randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			if (getLocalPlayer().getX() > 2599) {
				Walking.walk(beforec12049digspot.getRandomTile());
			} else if (getLocalPlayer().getX() < 2599) {
				Walking.walkExact(c12049digspot.getCenter());
				sleep(randomNum(200, 400));
			}
		}
		
		if (rangingguildtoc12049digspot.contains(getLocalPlayer()) && c12049digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7301() {
		currentClue = 7301;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!wizardtowerbasement.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!Inventory.contains("Key (medium)") && wizardtowerbig.contains(getLocalPlayer()) && !wizardtowerbeforestaircaseroombig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,4))) {
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(wizardtowerbeforestaircaseroomsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (!Inventory.contains("Key (medium)") && wizardtowerbig.contains(getLocalPlayer()) && wizardtowerbeforestaircaseroombig.contains(getLocalPlayer())) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				GameObject door = GameObjects.closest(f -> f.getName().contentEquals("Door") && f.hasAction("Open") && frontdoorwizardtower.contains(f));
				if (door != null) {
					door.interact("Open");
					sleepUntil(() -> infrontoffrontdoorwizardtower.contains(getLocalPlayer()), randomNum(3300, 4100));
					sleep(randomNum(700, 900));
				}
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC wizard = NPCs.closest(f -> f != null && f.getName().contentEquals("Wizard")); 
					if (wizard != null && Map.canReach(wizard)) {
						wizard.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (Inventory.contains("Key (medium)") && wizardtowerbig.contains(getLocalPlayer()) && !wizardtowerstaircaseroom.contains(getLocalPlayer()) && !wizardtowerbasement.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,4))) {
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(wizardtowerinsidestaircaseroomsmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (Inventory.contains("Key (medium)") && wizardtowerinside.contains(getLocalPlayer()) && wizardtowerstaircaseroom.contains(getLocalPlayer()) && !wizardtowerbasement.contains(getLocalPlayer())) {
			GameObject ladder = GameObjects.closest(f -> f.getName().contentEquals("Ladder") && f.hasAction("Climb-down"));
			if (ladder != null) {
				ladder.interact("Climb-down");
				sleepUntil(() -> wizardtowerbasement.contains(getLocalPlayer()), randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (Inventory.contains("Key (medium)") && wizardtowerbasement.contains(getLocalPlayer()) && !wizardtowerbasementdrawerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(wizardtowerbasementdrawersmall.getRandomTile());
			sleep(randomNum(300, 500));
		}

		if (Inventory.contains("Key (medium)") && wizardtowerbasement.contains(getLocalPlayer()) && wizardtowerbasementdrawerbig.contains(getLocalPlayer())) {
			GameObject drawers = GameObjects.closest(f -> f.getName().contentEquals("Drawers") && wizardtowerbasementdrawer.contains(f));
			if (drawers != null) {
				drawers.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 20) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
				
				if (Combat.isAutoRetaliateOn() == true) {
					sleep(randomNum(100,300));
			        Combat.toggleAutoRetaliate(false);
			        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
					sleep(randomNum(100,300));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12059() {
		currentClue = 12059;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdtonieve.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdtonieve.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("2");
				sleepUntil(() -> treegnomestrongholdspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !nievegnomestronghold.contains(getLocalPlayer()) && treegnomestrongholdtonieve.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
		
			Walking.walk(nievegnomestrongholdsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (nievegnomestronghold.contains(getLocalPlayer())) {
			NPC nieve = NPCs.closest("Nieve"); 
			if (nieve != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					nieve.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("2");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					nieve.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7286() {
		currentClue = 7286;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!cipto7286digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!cipto7286digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.MISCELLANIA);
					sleepUntil(() -> cipteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (cipto7286digspot.contains(getLocalPlayer()) && !c7286digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(5,7))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walkExact(c7286digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (cipto7286digspot.contains(getLocalPlayer()) && c7286digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3618() {
		currentClue = 3618;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!bugzone.contains(getLocalPlayer()) && !insiderantzcave.contains(getLocalPlayer()) && !akstorantzcave.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!insiderantzcave.contains(getLocalPlayer()) && !akstorantzcave.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.FELDIP_HILLS_HUNTER);
					sleepUntil(() -> feldiphillsteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (!insiderantzcave.contains(getLocalPlayer()) && akstorantzcave.contains(getLocalPlayer()) && !rantzcaveentrace.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(rantzcaveentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (rantzcaveentrace.contains(getLocalPlayer()) && !insiderantzcave.contains(getLocalPlayer())) {
			GameObject caveentrance = GameObjects.closest(f -> f.getName().contentEquals("Cave entrance") && f.hasAction("Enter"));
			if (caveentrance != null) {
				caveentrance.interact("Enter");
				sleepUntil(() -> insiderantzcave.contains(getLocalPlayer()), randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (insiderantzcave.contains(getLocalPlayer()) && !rantzcavemiddle.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(rantzcavemiddlesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (rantzcavemiddle.contains(getLocalPlayer())) {
			NPC fycie = NPCs.closest("Fycie"); 
			if (fycie != null) {
				fycie.interact("Talk");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7315() {
		currentClue = 7315;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!goldenappletreetoc7315digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!goldenappletreetoc7315digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.KANDARIN_SLAYER_CAVE );
					sleepUntil(() -> goldenappletree.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(6,9)) && goldenappletreetoc7315digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && !c7315digspot.contains(getLocalPlayer())) {
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walkExact(c7315digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (goldenappletreetoc7315digspot.contains(getLocalPlayer()) && c7315digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10272() {
		currentClue = 10272;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!outposttotrainingcamp.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "The Outpost");
			sleepUntil(() -> theoutpostteleport.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && outposttotrainingcamp.contains(getLocalPlayer()) && !trainingcampogrepen.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(trainingcampogrepensmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (trainingcampogrepen.contains(getLocalPlayer()) && !trainingcampogrepeninside.contains(getLocalPlayer())) {
			GameObject railing = GameObjects.closest(f -> f.getName().contentEquals("Loose Railing"));
			if (railing != null) {
				railing.interact("Squeeze-through");
				sleepUntil(() -> trainingcampogrepeninside.contains(getLocalPlayer()), randomNum(3300, 4100));
				sleep(randomNum(700, 900));
			}
		}
		
		if (!Inventory.contains("Steel sq shield") && !Equipment.contains("Steel sq shield") && trainingcampogrepeninside.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Mystic gloves") && !getLocalPlayer().isAnimating(), randomNum(5500,7000));
				sleep(randomNum(500, 800));
			}
		}
		
		int greendhidechaps = 0;
		int greendhidebody = 0;
		int steelsqshield = 0;
		
		if (Inventory.contains("Steel sq shield") && !Equipment.contains("Steel sq shield") && trainingcampogrepeninside.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			greendhidechaps = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
			Inventory.slotInteract(greendhidechaps, "Wear");
			sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			greendhidebody = Inventory.slot(f -> f.getName().contains("Green d'hide body"));
			Inventory.slotInteract(greendhidebody, "Wear");
			sleepUntil(() -> Equipment.contains("Green d'hide body"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			steelsqshield = Inventory.slot(f -> f.getName().contains("Steel sq shield"));
			Inventory.slotInteract(steelsqshield, "Wield");
			sleepUntil(() -> Equipment.contains("Steel sq shield"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Steel sq shield") && Equipment.contains("Steel sq shield") && trainingcampogrepeninside.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.CHEER);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.ANGRY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Steel sq shield") && Equipment.contains("Steel sq shield")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Steel sq shield") || Equipment.contains("Steel sq shield")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Steel sq shield")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						int greendhidebody1 = Inventory.slot(f -> f.getName().contains("Green d'hide body"));
						Inventory.slotInteract(greendhidebody1, "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide body"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int greendhidechaps1 = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
						Inventory.slotInteract(greendhidechaps1, "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Steel sq shield"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Steel sq shield") && !Equipment.contains("Steel sq shield")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Steel sq shield") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2821() {
		currentClue = 2821;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!faladortoc2821digspot.contains(getLocalPlayer())) {
			if (Inventory.contains("Falador teleport")) {
				Inventory.interact("Falador teleport", "Break");
				sleepUntil(() -> faladortp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (faladortoc2821digspot.contains(getLocalPlayer()) && !digspot2821.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(digspot2821.getCenter());
			sleep(randomNum(200,400));
		}
		
		if (faladortoc2821digspot.contains(getLocalPlayer()) && digspot2821.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19770() {
		currentClue = 19770;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!lumbridgegraveyardtoironman.contains(getLocalPlayer())) {
			if (Inventory.contains("Lumbridge graveyard teleport")) {
				Inventory.interact("Lumbridge graveyard teleport", "Break");
				sleepUntil(() -> lumbridgegraveyardtp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (lumbridgegraveyardtoironman.contains(getLocalPlayer()) && !ironmantutor.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ironmantutorsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (ironmantutor.contains(getLocalPlayer())) {
			NPC ironman = NPCs.closest("Iron Man tutor"); 
			if (ironman != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					ironman.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("666");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					ironman.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19780() {
		currentClue = 19780;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !draynorvillagetojail.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(600, 800));
			Dialogues.chooseOption(3);
			sleepUntil(() -> draynorvillagetp.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(120, 300));
		}
		
		if (!Inventory.contains("Adamant plateskirt") && Walking.shouldWalk(randomNum(6,9)) && !draynorjailoutside.contains(getLocalPlayer()) && draynorvillagetojail.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(draynorjailoutsidesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Adamant plateskirt") && !Equipment.contains("Adamant plateskirt") && draynorjailoutside.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Adamant plateskirt") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int sapphireamulet = 0;
		int adamantplateskirt = 0;
		
		if (Inventory.contains("Adamant plateskirt") && !Equipment.contains("Adamant plateskirt") && draynorjailoutside.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Adamant sword", "Wield");
			sleepUntil(() -> Equipment.contains("Adamant sword"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			sapphireamulet = Inventory.slot(f -> f.getName().contains("Sapphire amulet"));
			Inventory.slotInteract(sapphireamulet, "Wear");
			sleepUntil(() -> Equipment.contains("Sapphire amulet"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			adamantplateskirt = Inventory.slot(f -> f.getName().contains("Adamant plateskirt"));
			Inventory.slotInteract(adamantplateskirt, "Wear");
			sleepUntil(() -> !Equipment.contains("Adamant plateskirt"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Adamant plateskirt") && Equipment.contains("Adamant plateskirt") && !draynorjailinside.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(2,3))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(draynorjailinsidesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Adamant plateskirt") && Equipment.contains("Adamant plateskirt") && draynorjailinside.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.CRY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.JUMP_FOR_JOY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));

			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Adamant plateskirt") && Equipment.contains("Adamant plateskirt")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Adamant plateskirt") || Equipment.contains("Adamant plateskirt")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (!Inventory.contains("Adamant plateskirt") && Equipment.contains("Adamant plateskirt")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Adamant sword"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int necklaceofpassage = Inventory.slot(f -> f.getName().contains("Necklace of passage"));
						Inventory.slotInteract(necklaceofpassage, "Wear");
						sleepUntil(() -> !Equipment.contains("Sapphire amulet"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int greendhidechaps1 = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
						Inventory.slotInteract(greendhidechaps1, "Wear");
						sleepUntil(() -> !Equipment.contains("Adamant plateskirt"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Adamant plateskirt") && !Equipment.contains("Adamant plateskirt") && !draynorjailoutside.contains(getLocalPlayer())) {
						if (Walking.shouldWalk(randomNum(2,4))) {
							Walking.walk(draynorjailoutsidesmall.getRandomTile());
							sleep(randomNum(700, 850));
						}
					}
					
					while (Inventory.contains("Adamant plateskirt") && !Equipment.contains("Adamant plateskirt") && draynorjailoutside.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Adamant plateskirt") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7309() {
		currentClue = 7309;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!dksto7309digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!dksto7309digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.KARAMJA_SOUTH_MUSA_POINT);
					sleepUntil(() -> dksto7309digspot.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (dksto7309digspot.contains(getLocalPlayer()) && !c7309digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c7309digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (dksto7309digspot.contains(getLocalPlayer()) && c7309digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19746() {
		currentClue = 19746;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!lumbridgegraveyardtochurch.contains(getLocalPlayer())) {
			if (Inventory.contains("Lumbridge graveyard teleport")) {
				Inventory.interact("Lumbridge graveyard teleport", "Break");
				sleepUntil(() -> lumbridgegraveyardtp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (lumbridgegraveyardtochurch.contains(getLocalPlayer()) && !lumbridgechurch.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(lumbridgechurchsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (lumbridgegraveyardtochurch.contains(getLocalPlayer()) && lumbridgechurch.contains(getLocalPlayer())) {
			NPC priest = NPCs.closest("Father Aereck"); 
			if (priest != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					priest.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("19");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					priest.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3594() {
		currentClue = 3594;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto3594.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto3594.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("2");
				sleepUntil(() -> treegnomestrongholdspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !digspot3594.contains(getLocalPlayer()) && treegnomestrongholdto3594.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
		
			Walking.walkExact(digspot3594.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (digspot3594.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(10, 30));
		}
		
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7300() {
		currentClue = 7300;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!ardougnetowitchaven.contains(getLocalPlayer()) && !seaslugplatform.contains(getLocalPlayer())) {
			if (Inventory.contains("Ardougne teleport")) {
				Inventory.interact("Ardougne teleport", "Break");
				sleepUntil(() -> ardougnemarket.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(430, 600));
			}
		}
		
		if (!seaslugplatform.contains(getLocalPlayer()) && ardougnetowitchaven.contains(getLocalPlayer()) && !witchavennorth2.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(witchavennorth2small.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (witchavennorth2.contains(getLocalPlayer())) {
			NPC holgart = NPCs.closest("Holgart"); 
			if (holgart != null) {
				holgart.interact("Travel");
				sleepUntil(() -> seaslugplatform.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));	
			}
		}
		
		if (seaslugplatform.contains(getLocalPlayer()) && !seaslugplatformhouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(seaslugplatformhousesmall.getRandomTile());
			sleep(randomNum(232,540));
		}
		
		if (seaslugplatformhouse.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate"));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2851() {
		currentClue = 2851;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!monasterytomountain.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Combat bracelet("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.HANDS, "Monastery");
			sleepUntil(() -> monasterytp.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (monasterytomountain.contains(getLocalPlayer()) && !whitewolfmountaintop.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(whitewolfmountaintopsmall.getRandomTile());
			sleep(randomNum(232,540));
		}
		
		if (whitewolfmountaintop.contains(getLocalPlayer())) {
			NPC oracle = NPCs.closest("Oracle"); 
			if (oracle != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					oracle.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("48");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					oracle.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12023() {
		currentClue = 12023;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			C12067teleportbugfix = 0;
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!catherbytocamelot.contains(getLocalPlayer()) && Inventory.contains("Camelot teleport")) {
			if (C12067teleportbugfix == 0) {
				Inventory.interact("Camelot teleport", "Break");
				sleepUntil(() -> camelotteleport.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(330, 500));
				if (catherbytocamelot.contains(getLocalPlayer())) {
					C12067teleportbugfix = 1;
				}
			}
		}
		
		if (!Inventory.contains("Iron med helm") && catherbytocamelot.contains(getLocalPlayer()) && !catherbybankoutside.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(catherbybankoutsidesmall.getRandomTile());
			sleep(randomNum(232,540));
		}
		

		if (!Inventory.contains("Iron med helm") && !Equipment.contains("Iron med helm") && catherbybankoutside.contains(getLocalPlayer())) {
			C12067teleportbugfix = 0;
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Iron med helm") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int greendhide = 0;
		
		if (Inventory.contains("Iron med helm") && !Equipment.contains("Iron med helm") && catherbybankoutside.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Maple longbow", "Wield");
			sleepUntil(() -> Equipment.contains("Maple longbow"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			greendhide = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
			Inventory.slotInteract(greendhide, "Wear");
			sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Iron med helm", "Wear");
			sleepUntil(() -> !Equipment.contains("Iron med helm"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Iron med helm") && Equipment.contains("Iron med helm") && !catherbybankinside.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(2,3))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(catherbybankinsidesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Iron med helm") && Equipment.contains("Iron med helm") && catherbybankinside.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.SHRUG);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.YAWN);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));

			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Iron med helm") && Equipment.contains("Iron med helm")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Iron med helm") || Equipment.contains("Iron med helm")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (!Inventory.contains("Iron med helm") && Equipment.contains("Iron med helm")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Maple longbow"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int greendhidechaps1 = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
						Inventory.slotInteract(greendhidechaps1, "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int kandarinheadgear1 = Inventory.slot(f -> f.getName().contains("Kandarin headgear 1"));
						Inventory.slotInteract(kandarinheadgear1, "Wear");
						sleepUntil(() -> !Equipment.contains("Iron med helm"), randomNum(3500,5000));
						sleep(randomNum(100, 200));

					}
					
					while (Inventory.contains("Iron med helm") && !Equipment.contains("Iron med helm") && !catherbybankoutside.contains(getLocalPlayer())) {
						if (Walking.shouldWalk(randomNum(2,4))) {
							Walking.walk(catherbybankoutsidesmall.getRandomTile());
							sleep(randomNum(700, 850));
						}
					}
					
					while (Inventory.contains("Iron med helm") && !Equipment.contains("Iron med helm") && catherbybankoutside.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Iron med helm") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7278() {
		currentClue = 7278;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!westardougnetosquare.contains(getLocalPlayer())) {
			if (Inventory.contains("West ardougne teleport")) {
				Inventory.interact("West ardougne teleport", "Break");
				sleepUntil(() -> westardougneteleport.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
        }
		
		if (westardougnetosquare.contains(getLocalPlayer()) && !westardougnesquare.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(westardougnesquaresmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (westardougnesquare.contains(getLocalPlayer())) {
			NPC jethick = NPCs.closest("Jethick"); 
			if (jethick != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					jethick.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("38");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					jethick.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10276() {
		currentClue = 10276;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			C12067teleportbugfix = 0;
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!catherbytocamelot2.contains(getLocalPlayer()) && Inventory.contains("Camelot teleport")) {
			if (C12067teleportbugfix == 0) {
				Inventory.interact("Camelot teleport", "Break");
				sleepUntil(() -> camelotteleport.contains(getLocalPlayer()), randomNum(2500,3500));
				sleep(randomNum(730, 900));
				if (catherbytocamelot2.contains(getLocalPlayer())) {
					C12067teleportbugfix = 1;
				}
			}
		}
		
		if (!Inventory.contains("Silver sickle") && catherbytocamelot2.contains(getLocalPlayer()) && !catherbyranging.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(catherbyrangingsmall.getRandomTile());
			sleep(randomNum(232,540));
		}
		
		if (!Inventory.contains("Silver sickle") && !Equipment.contains("Silver sickle") && catherbyranging.contains(getLocalPlayer())) {
			C12067teleportbugfix = 0;
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Silver sickle") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
				
		if (Inventory.contains("Silver sickle") && !Equipment.contains("Silver sickle") && catherbyranging.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Blue boots", "Wear");
			sleepUntil(() -> Equipment.contains("Blue boots"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Hardleather body", "Wear");
			sleepUntil(() -> Equipment.contains("Hardleather body"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Silver sickle", "Wield");
			sleepUntil(() -> Equipment.contains("Silver sickle"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Silver sickle") && Equipment.contains("Silver sickle") && catherbyranging.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.CRY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.BOW);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));

			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Silver sickle") && Equipment.contains("Silver sickle")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Silver sickle") || Equipment.contains("Silver sickle")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (!Inventory.contains("Silver sickle") && Equipment.contains("Silver sickle")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						
						int bootsoflightness = Inventory.slot(f -> f.getName().contains("Boots of lightness"));
						Inventory.slotInteract(bootsoflightness, "Wear");
						sleepUntil(() -> Equipment.contains("Boots of lightness"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int greendhide = Inventory.slot(f -> f.getName().contains("Green d'hide body"));
						Inventory.slotInteract(greendhide, "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide body"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
						Inventory.slotInteract(magicshortbow, "Wield");
						sleepUntil(() -> !Equipment.contains("Silver sickle"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Silver sickle") && !Equipment.contains("Silver sickle")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Silver sickle") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12041() {
		currentClue = 12041;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!bkpto12041digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!bkpto12041digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SW_CASTLE_WARS );
					sleepUntil(() -> bkpteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (bkpto12041digspot.contains(getLocalPlayer()) && !c12041digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c12041digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (bkpto12041digspot.contains(getLocalPlayer()) && c12041digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10266() {
		currentClue = 10266;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto10266.contains(getLocalPlayer()) && !gnomeagilityarenaz1.contains(getLocalPlayer()) && !gnomeagilityarenaz2.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdto10266.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("2");
				sleepUntil(() -> treegnomestrongholdspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !gnomeagilityarenaz0.contains(getLocalPlayer()) && treegnomestrongholdto10266.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
		
			Walking.walk(gnomeagilityarenaz0small.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (gnomeagilityarenaz0.contains(getLocalPlayer())) {
			GameObject obstacle = GameObjects.closest(f -> f.getName().contentEquals("Obstacle net") && obstaclenetsagility.contains(f));
			if (obstacle != null) {
				sleep(randomNum(150, 300));
				obstacle.interact("Climb-over");
				sleepUntil(() -> gnomeagilityarenaz1.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (gnomeagilityarenaz1.contains(getLocalPlayer())) {
			GameObject obstacle2 = GameObjects.closest(f -> f.getName().contentEquals("Tree branch"));
			if (obstacle2 != null) {
				sleep(randomNum(150, 300));
				obstacle2.interact("Climb");
				sleepUntil(() -> gnomeagilityarenaz2.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (gnomeagilityarenaz2.contains(getLocalPlayer())) {
			if (!Inventory.contains("Ring of forging") && !Equipment.contains("Ring of forging")) {
				GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
				if (STASH != null) {
					sleep(randomNum(150, 300));
					STASH.interact("Search");
					sleepUntil(() -> Inventory.contains("Ring of forging") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
					sleep(randomNum(500, 800));
				}
			}
			
			
			int greendhidechaps = 0;
			
			if (Inventory.contains("Ring of forging") && !Equipment.contains("Ring of forging")) {
				if(Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(73,212));
				}
				greendhidechaps = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
				Inventory.slotInteract(greendhidechaps, "Wear");
				sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
				sleep(randomNum(100, 200));
				Inventory.interact("Steel kiteshield", "Wear");
				sleepUntil(() -> Equipment.contains("Steel kiteshield"), randomNum(3500,5000));
				sleep(randomNum(100, 200));
				Inventory.interact("Ring of forging", "Wear");
				sleepUntil(() -> Equipment.contains("Ring of forging"), randomNum(3500,5000));
				sleep(randomNum(100, 200));
			}
			
			if (!Inventory.contains("Ring of forging") && Equipment.contains("Ring of forging")) {
				Emotes.doEmote(Emote.CRY);
				sleep(randomNum(700, 850));
				sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
				sleep(randomNum(60, 150));
				Emotes.doEmote(Emote.NO);
				sleep(randomNum(700, 850));
				sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
				sleep(randomNum(60, 150));

				
				NPC uri = NPCs.closest("Uri");
				if (uri == null) {
					sleepUntil(() -> uri != null, randomNum(2300, 3400));
					sleep(randomNum(100,300));
				}
				
				if (uri != null && !Inventory.contains("Ring of forging") && Equipment.contains("Ring of forging")) {
					uri.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
					sleep(randomNum(100,300));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
					sleep(randomNum(100,300));
					Dialogues.continueDialogue();
					while (Inventory.contains("Ring of forging") || Equipment.contains("Ring of forging")) {
						int i = 0;
						while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
							i++;
							sleep(randomNum(180,220));
						}
						sleep(randomNum(20,50));
						
						while (!Inventory.contains("Ring of forging") && Equipment.contains("Ring of forging")) {
							if(Tabs.getOpen() != Tab.INVENTORY) {
								Tabs.open(Tab.INVENTORY);
								sleep(randomNum(73,212));
							}
							
							int greendhide = Inventory.slot(f -> f.getName().contains("Green d'hide chaps"));
							Inventory.slotInteract(greendhide, "Wear");
							sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500,5000));
							sleep(randomNum(100, 200));
							int magicshortbow = Inventory.slot(f -> f.getName().contains("Magic shortbow"));
							Inventory.slotInteract(magicshortbow, "Wield");
							sleepUntil(() -> Equipment.contains("Magic shortbow"), randomNum(3500,5000));
							sleep(randomNum(100, 200));
							int ringofwealth = Inventory.slot(f -> f.getName().contains("Ring of wealth"));
							Inventory.slotInteract(ringofwealth, "Wear");
							sleepUntil(() -> !Equipment.contains("Ring of forging"), randomNum(3500,5000));
							sleep(randomNum(100, 200));
						}
						
						while (Inventory.contains("Ring of forging") && !Equipment.contains("Ring of forging")) {
							GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
							if (STASH != null) {
								STASH.interact("Search");
								sleepUntil(() -> !Inventory.contains("Ring of forging") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
								sleep(randomNum(500, 800));
							}
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12071() {
		currentClue = 12071;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !burthorpecastlez1.contains(getLocalPlayer()) && !burthorpetocastlez0.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> burthorpeteleport.contains(getLocalPlayer()), randomNum(3500,5000));
			sleep(randomNum(520, 800));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && burthorpetocastlez0.contains(getLocalPlayer()) && !insideburthorpecastlez0.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(burthorpecastlestairs.getRandomTile());
			sleep(randomNum(200, 400));
			
			GameObject largedoor = GameObjects.closest(f -> f.getName().contentEquals("Large door") && burthorpecastledoor.contains(f) && f.hasAction("Open"));
			if (largedoor != null && largedoor.getTile().distance() <= 14) {
				largedoor.interact("Open");
				sleepUntil(() -> tileinfrontofdoor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (insideburthorpecastlez0.contains(getLocalPlayer())) {
			GameObject stairs = GameObjects.closest(f -> f.getName().contentEquals("Stairs") && f.hasAction("Climb-up"));
			if (stairs != null) {
				stairs.interact("Climb-up");
				sleepUntil(() -> burthorpecastlez1.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (burthorpecastlez1.contains(getLocalPlayer())) {
			NPC eohric = NPCs.closest("Eohric"); 
			if (eohric != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					eohric.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(400, 600));
					Dialogues.continueDialogue();
					sleep(randomNum(400, 600));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("36");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					eohric.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2856() {
		currentClue = 2856;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!faladortopartyroom.contains(getLocalPlayer())) {
			if (Inventory.contains("Falador teleport")) {
				Inventory.interact("Falador teleport", "Break");
				sleepUntil(() -> faladortp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && faladortopartyroom.contains(getLocalPlayer()) && !partyroom.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			GameObject castledoor = GameObjects.closest(f -> f.getName().contentEquals("Castle door") && f.hasAction("Open"));
			if (castledoor != null && (castledoor.distance() <= 13)) {
				castledoor.interact("Open");
				sleepUntil(() -> faladorcastledoor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (getLocalPlayer().getX() <= 3012) {
				Walking.walk(onthewaytopartyroom.getRandomTile());
				sleep(randomNum(200, 400));
			} else if (getLocalPlayer().getX() > 3012 && getLocalPlayer().getX() <= 3034) {
				Walking.walk(onthewaytopartyroom2.getRandomTile());
				sleep(randomNum(200, 400));
			} else if (getLocalPlayer().getX() > 3034) {
				Walking.walk(partyroomsmall.getRandomTile());
				sleep(randomNum(200, 400));	
			}
		}
		
		if (partyroom.contains(getLocalPlayer())) {
			NPC partypete = NPCs.closest("Party Pete"); 
			if (partypete != null) {
				partypete.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}	
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2815() {
		currentClue = 2815;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !karamjatovolcano.contains(getLocalPlayer()) && !karamjaunderground.contains(getLocalPlayer()) && !crandor.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(2);
			sleepUntil(() -> karamjateleport.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(120, 300));
		}
		
		if (karamjatovolcano.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(karamjaundergroundentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));

			GameObject entrancerocks = GameObjects.closest("Rocks");
			if (entrancerocks != null && entrancerocks.getTile().distance() <= 10) {
				entrancerocks.interact("Climb-down");
				sleepUntil(() -> karamjaunderground.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (karamjaunderground.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			if (getLocalPlayer().getY() <= 9593) {
				Walking.walk(crandorentranceareadoor.getRandomTile());
				sleep(randomNum(200, 400));
			}
			
			if (getLocalPlayer().getY() > 9593) {
				Walking.walk(crandorentranceareasmall.getRandomTile());
				sleep(randomNum(200, 400));
			}

			GameObject climbingrope = GameObjects.closest(f -> f.getName().contentEquals("Climbing rope") && climbingropecrandor.contains(f));
			if (climbingrope != null && climbingrope.getTile().distance() <= 15) {
				climbingrope.interact("Climb");
				sleepUntil(() -> crandor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}

			if (!pastkaramjadungeondoor.contains(getLocalPlayer())) {
				GameObject karamjadungeondoor = GameObjects.closest(f -> f.getName().contentEquals("Wall"));
				if (karamjadungeondoor != null && karamjadungeondoor.getTile().distance() <= 15) {
					karamjadungeondoor.interact("Open");
					sleepUntil(() -> afterdoor.contains(getLocalPlayer()), randomNum(5300, 6500));
					sleep(randomNum(150, 300));
				} 
			}
			
			if (getLocalPlayer().getHealthPercent() <= 40 && Inventory.contains("Shark")) {
				Inventory.interact("Shark", "Eat");
				sleep(randomNum(200, 400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && crandor.contains(getLocalPlayer()) && !C2815digspot.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(C2815digspot.getCenter());
			sleep(randomNum(200, 400));
		}

		if (C2815digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23139() {
		currentClue = 23139;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !faladorparkbig.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Falador");
			sleepUntil(() -> faladorparktp.contains(getLocalPlayer()), randomNum(1500,1700));
			sleep(randomNum(932,1240));
		}

		if (faladorparkbig.contains(getLocalPlayer()) && !faladorparkbridge.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(faladorparkbridge.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (faladorparkbridge.contains(getLocalPlayer()) && faladorparkbig.contains(getLocalPlayer())) {
			NPC cecilia = NPCs.closest("Cecilia"); 
			if (cecilia != null) {
				cecilia.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(3500, 4500));
				sleep(randomNum(123,411));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.getOptionIndexContaining("Yes, I have.") != -1, randomNum(3500, 4500));
				sleep(randomNum(115,432));
				Dialogues.chooseOption(1);
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
			}
			
			if (Tabs.getOpen() != Tab.MUSIC) {
				Tabs.open(Tab.MUSIC);
				sleep(randomNum(200,400));
			}

			int scrollx = Widgets.getWidget(239).getChild(4).getChild(0).getX();
			int scrolly = Widgets.getWidget(239).getChild(4).getChild(0).getY();
			int scrollheight = Widgets.getWidget(239).getChild(4).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(239).getChild(4).getChild(0).getWidth();
			//Mouse.click(new Point(randomNum(scrollx+(scrollwidth/2)-1,scrollx+(scrollwidth/2)+1), randomNum((int)(scrolly+(scrollheight/3.5)-1),(int)(scrolly+(scrollheight/3.5)+1))));
			Mouse.click(new Point(scrollx+(scrollwidth/2), (int) (scrolly+(scrollheight/3.5))));
			sleep(randomNum(135,442));
			Widgets.getWidget(239).getChild(3).getChild(133).interact("Play");

			if (cecilia != null) {
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(15, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2807() {
		currentClue = 2807;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!outpostto2807digspot.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "The Outpost");
			sleepUntil(() -> theoutpostteleport.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && outpostto2807digspot.contains(getLocalPlayer()) && !c2807digspot.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
		 
			Walking.walk(c2807digspot.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (outpostto2807digspot.contains(getLocalPlayer()) && c2807digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7313() {
		currentClue = 7313;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!lumbridgegraveyardtoswamp2.contains(getLocalPlayer()) && Inventory.contains("Lumbridge graveyard teleport")) {
			Inventory.interact("Lumbridge graveyard teleport", "Break");
			sleepUntil(() -> lumbridgegraveyardtp.contains(getLocalPlayer()), randomNum(3000,4000));
			sleep(randomNum(200,400));
		}
		
		if (lumbridgegraveyardtoswamp2.contains(getLocalPlayer()) && !c7313digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c7313digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (lumbridgegraveyardtoswamp2.contains(getLocalPlayer()) && c7313digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2823() {
		currentClue = 2823;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!lumbridgegraveyardtoswamp2.contains(getLocalPlayer()) && Inventory.contains("Lumbridge graveyard teleport")) {
			Inventory.interact("Lumbridge graveyard teleport", "Break");
			sleepUntil(() -> lumbridgegraveyardtp.contains(getLocalPlayer()), randomNum(3000,4000));
			sleep(randomNum(200,400));
		}
		
		if (lumbridgegraveyardtoswamp2.contains(getLocalPlayer()) && !c2823digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c2823digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (lumbridgegraveyardtoswamp2.contains(getLocalPlayer()) && c2823digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3612() {
		currentClue = 3612;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !insideswcave.contains(getLocalPlayer()) && !grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdtoswcave.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !treegnomestrongholdtoswcave.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("2");
				sleepUntil(() -> treegnomestrongholdspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !strongholdswcaveentrance.contains(getLocalPlayer()) && treegnomestrongholdtoswcave.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
		
			Walking.walk(strongholdswcaveentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (strongholdswcaveentrance.contains(getLocalPlayer())) {
			GameObject caveentrance = GameObjects.closest(f -> f.getName().contentEquals("Cave entrance"));
			if (caveentrance != null) {
				caveentrance.interact("Enter");
				sleepUntil(() -> insideswcave.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(332,640));
			}
		}
		
		if (insideswcave.contains(getLocalPlayer())) {
			NPC brimstail = NPCs.closest("Brimstail"); 
			if (brimstail != null) {
				brimstail.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12065() {
		currentClue = 12065;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!faladortoinn.contains(getLocalPlayer())) {
			if (Inventory.contains("Falador teleport")) {
				Inventory.interact("Falador teleport", "Break");
				sleepUntil(() -> faladortp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && faladortoinn.contains(getLocalPlayer()) && !risingsuninn.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(risingsuninnsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (risingsuninn.contains(getLocalPlayer())) {
			NPC kaylee = NPCs.closest("Kaylee"); 
			if (kaylee != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					kaylee.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("18");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					kaylee.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12057() {
		currentClue = 12057;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!varrockcenterlarge.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			
			Inventory.interact("Varrock teleport", "Break");
			sleepUntil(() -> varrockcentre.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(100,400));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && varrockcenterlarge.contains(getLocalPlayer()) && !bareakarea.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(bareakareasmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (bareakarea.contains(getLocalPlayer())) {
			NPC baraek = NPCs.closest("Baraek"); 
			if (baraek != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					baraek.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("5");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					baraek.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19774() {
		currentClue = 19774;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!djrtoc19774digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!djrtoc19774digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.enterCode(0, "D");
					sleep(randomNum(150, 300));
					FairyRings.enterCode(1, "J");
					sleep(randomNum(150, 300));
					FairyRings.enterCode(2, "R");
					sleep(randomNum(150, 300));
					Widgets.getWidget(398).getChild(26).interact("Confirm");
					sleep(randomNum(60, 150));
					sleepUntil(() -> djrteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (djrtoc19774digspot.contains(getLocalPlayer()) && !c19774digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c19774digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (djrtoc19774digspot.contains(getLocalPlayer()) && c19774digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7290() {
		currentClue = 7290;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !battlefieldofkhazadtoouraniacave.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !battlefieldofkhazadtoouraniacave.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("3");
				sleepUntil(() -> khazadspirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !digspot7290.contains(getLocalPlayer()) && battlefieldofkhazadtoouraniacave.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
		
			Walking.walkExact(digspot7290.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (digspot7290.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(170,220));
			}
			sleep(randomNum(10, 30));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7303() {
		currentClue = 7303;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!pohtodesertmine.contains(getLocalPlayer()) || desertminejail.contains(getLocalPlayer())) {
			if (Inventory.contains("Teleport to house")) {
				Inventory.interact("Teleport to house", "Break");
				sleepUntil(() -> pohpolniveachtp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
        }
		
		
		if (pohtodesertmine.contains(getLocalPlayer()) && !desertmineentrance.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(desertmineentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (desertmineentrance.contains(getLocalPlayer())) {
			if (Equipment.contains("Kandarin headgear 1")) {
				sleep(randomNum(100, 300));
				Equipment.unequip(EquipmentSlot.HAT);
				sleepUntil(() -> !Equipment.contains("Kandarin headgear 1"), randomNum(3500, 5000));
				sleep(randomNum(100, 300));
			}
			if (Equipment.contains("Green d'hide body")) {
				sleep(randomNum(20, 70));
				Equipment.unequip(EquipmentSlot.CHEST);
				sleepUntil(() -> !Equipment.contains("Green d'hide body"), randomNum(3500, 5000));
				sleep(randomNum(100, 300));
			}
			if (Equipment.contains("Green d'hide chaps")) {
				sleep(randomNum(20, 70));
				Equipment.unequip(EquipmentSlot.LEGS);
				sleepUntil(() -> !Equipment.contains("Green d'hide chaps"), randomNum(3500, 5000));
				sleep(randomNum(100, 300));
			}
			if (Equipment.contains("Boots of lightness")) {
				sleep(randomNum(20, 70));
				Equipment.unequip(EquipmentSlot.FEET);
				sleepUntil(() -> !Equipment.contains("Boots of lightness"), randomNum(3500, 5000));
				sleep(randomNum(100, 300));
			}
			if (Equipment.contains("Magic shortbow")) {
				sleep(randomNum(20, 70));
				Equipment.unequip(EquipmentSlot.WEAPON);
				sleepUntil(() -> !Equipment.contains("Magic shortbow"), randomNum(3500, 5000));
				sleep(randomNum(100, 300));
			}
			
			if (!Equipment.contains("Kandarin headgear 1") && !Equipment.contains("Green d'hide body") && !Equipment.contains("Green d'hide chaps") && !Equipment.contains("Boots of lightness") && !Equipment.contains("Magic shortbow")) {
				GameObject gate = GameObjects.closest("Gate");
				if (gate != null) {
					gate.interact("Open");
					sleepUntil(() -> desertmineinside.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(1050, 1300));
				}
			}
		}
		
		if (desertmineinside.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) && !desertminecrate.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(desertminecratesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (desertminecrate.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate") && desertminecratetile.contains(f));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
				
				while (!Equipment.contains("Kandarin headgear 1") || !Equipment.contains("Green d'hide body") || !Equipment.contains("Green d'hide chaps") || !Equipment.contains("Boots of lightness") || !Equipment.contains("Magic shortbow")) {
					if (!Equipment.contains("Kandarin headgear 1")) {
						sleep(randomNum(100, 300));
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> Equipment.contains("Kandarin headgear 1"), randomNum(3500, 5000));
						sleep(randomNum(100, 300));
					}
					if (!Equipment.contains("Green d'hide body")) {
						sleep(randomNum(20, 70));
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide body"), randomNum(3500, 5000));
						sleep(randomNum(100, 300));
					}
					if (!Equipment.contains("Green d'hide chaps")) {
						sleep(randomNum(20, 70));
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> Equipment.contains("Green d'hide chaps"), randomNum(3500, 5000));
						sleep(randomNum(100, 300));
					}
					if (!Equipment.contains("Boots of lightness")) {
						sleep(randomNum(20, 70));
						Inventory.interact("Boots of lightness", "Wear");
						sleepUntil(() -> Equipment.contains("Boots of lightness"), randomNum(3500, 5000));
						sleep(randomNum(100, 300));
					}
					if (!Equipment.contains("Magic shortbow")) {
						sleep(randomNum(20, 70));
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> Equipment.contains("Magic shortbow"), randomNum(3500, 5000));
						sleep(randomNum(100, 300));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19778() {
		currentClue = 19778;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!arceuustolibrary.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!arceuustolibrary.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.ARCEUUS_LIBRARY);
					sleepUntil(() -> arceuuslibraryteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (arceuustolibrary.contains(getLocalPlayer()) && !arceuuslibrarystash.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(arceuuslibrarystashsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Adamant dagger") && !Equipment.contains("Adamant dagger") && arceuuslibrarystash.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Adamant dagger") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		int bluedhide = 0;
		int adamantboots = 0;
		int adamantdagger = 0;
		
		if (Inventory.contains("Adamant dagger") && !Equipment.contains("Adamant dagger") && arceuuslibrarystash.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			bluedhide = Inventory.slot(f -> f.getName().contains("Blue d'hide vambraces"));
			Inventory.slotInteract(bluedhide, "Wear");
			sleepUntil(() -> Equipment.contains("Blue d'hide vambraces"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			adamantboots = Inventory.slot(f -> f.getName().contains("Adamant boots"));
			Inventory.slotInteract(adamantboots, "Wear");
			sleepUntil(() -> Equipment.contains("Adamant boots"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			adamantdagger = Inventory.slot(f -> f.getName().contains("Adamant dagger"));
			Inventory.slotInteract(adamantdagger, "Wield");
			sleepUntil(() -> Equipment.contains("Adamant dagger"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Adamant dagger") && Equipment.contains("Adamant dagger") && arceuuslibrary.contains(getLocalPlayer()) && !arceuuslibrarymiddle.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(1,3))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(arceuuslibrarymiddlesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Adamant dagger") && Equipment.contains("Adamant dagger") && arceuuslibrarymiddle.contains(getLocalPlayer())) {			
			Emotes.doEmote(Emote.YAWN);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.YES);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Adamant dagger") && Equipment.contains("Adamant dagger")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Adamant dagger") || Equipment.contains("Adamant dagger")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Adamant dagger") && !Inventory.contains("Adamant dagger")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						int combatbracelet = Inventory.slot(f -> f.getName().contains("Combat bracelet"));
						Inventory.slotInteract(combatbracelet, "Wear");
						sleepUntil(() -> !Equipment.contains("Blue d'hide vambraces"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int bootsoflightness = Inventory.slot(f -> f.getName().contains("Boots of lightness"));
						Inventory.slotInteract(bootsoflightness, "Wear");
						sleepUntil(() -> !Equipment.contains("Adamant boots"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> !Equipment.contains("Adamant dagger"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (Inventory.contains("Adamant dagger") && !Equipment.contains("Adamant dagger")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Adamant dagger") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2839() {
		currentClue = 2839;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!yanilleinsidelargehousez1.contains(getLocalPlayer()) && !yanillefairytobank.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!yanillefairytobank.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.NW_YANILLE);
					sleepUntil(() -> nwayanilletp.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (!Inventory.contains("Key (medium)") && yanillefairytobank.contains(getLocalPlayer()) && !yanillelargehouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			GameObject largedoor2 = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && yanillefirstdoor.contains(f));
			if (largedoor2 != null && largedoor2.getTile().distance() <= 10 && yanilleoutside.contains(getLocalPlayer())) {
				largedoor2.interact("Open");
				sleepUntil(() -> infrontoflargedoor2.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			GameObject largedoor = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && yanilleseconddoor.contains(f));
			if (largedoor != null && largedoor.getTile().distance() <= 10 && yanilleairlock.contains(getLocalPlayer())) {
				largedoor.interact("Open");
				sleepUntil(() -> infrontoflargedoor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			Walking.walk(yanilleoutsidelargehousesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}

		if (!Inventory.contains("Key (medium)") && yanillelargehouse.contains(getLocalPlayer())) {
			GameObject largedoor = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && yanillethirddoor.contains(f));
			if (largedoor != null) {
				largedoor.interact("Open");
				sleepUntil(() -> yanillelargehousedoor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC man = NPCs.closest(f -> f != null && f.getName().contentEquals("Man")); 
					if (man != null && Map.canReach(man)) {
						man.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (Inventory.contains("Key (medium)") && yanilleinside.contains(getLocalPlayer()) && !yanilleinsidelargehouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			GameObject largedoor = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && yanillethirddoor.contains(f));
			if (largedoor != null && largedoor.getTile().distance() <= 10 && yanilleoutsidelargehouse.contains(getLocalPlayer())) {
				largedoor.interact("Open");
				sleepUntil(() -> yanillelargehousedoor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			Walking.walk(yanilleinsidelargehousesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (Inventory.contains("Key (medium)") && yanilleinsidelargehouse.contains(getLocalPlayer())) {
			GameObject ladder = GameObjects.closest(f -> f.hasAction("Climb-up") && f.getName().contentEquals("Ladder"));
			if (ladder != null) {
				ladder.interact("Climb-up");
				sleepUntil(() -> yanilleinsidelargehousez1.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (Inventory.contains("Key (medium)") && yanilleinsidelargehousez1.contains(getLocalPlayer())) {
			GameObject chest = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Closed chest"));
			if (chest != null) {
				chest.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12037() {
		currentClue = 12037;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		 
		if (!fenkenstrainscastletoc12037.contains(getLocalPlayer()) && Inventory.contains("Fenkenstrain's castle teleport")) {
			Inventory.interact("Fenkenstrain's castle teleport", "Break");
			sleepUntil(() -> fenkenstrainscastleteleport.contains(getLocalPlayer()), randomNum(3000,4000));
			sleep(randomNum(200,400));
	    }
		
		if (fenkenstrainscastletoc12037.contains(getLocalPlayer()) && !c12037digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (getLocalPlayer().getY() <= 3534) {
				Walking.walk(new Tile(3548, 3535));
				sleep(randomNum(200, 400));
			} else if (getLocalPlayer().getY() > 3534) {
				Walking.walkExact(c12037digspot.getCenter());
				sleep(randomNum(200, 400));
			}
		}
		
		if (fenkenstrainscastletoc12037.contains(getLocalPlayer()) && c12037digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3586() {
		currentClue = 3586;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !burthorpeto3586digspot.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> burthorpeteleport.contains(getLocalPlayer()), randomNum(3500,5000));
			sleep(randomNum(520, 800));
		}

		if (Walking.shouldWalk(randomNum(4,6)) && burthorpeto3586digspot.contains(getLocalPlayer()) && !c3586digspot.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (stucktileburthorpe.contains(getLocalPlayer())) {
				Walking.walk(unstucktileburthorpe.getCenter());
			} else {
				Walking.walkExact(c3586digspot.getCenter());
				sleep(randomNum(200, 400));
			}
		}
		
		if (burthorpeto3586digspot.contains(getLocalPlayer()) && c3586digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23142() {
		currentClue = 23142;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !faladorparkbig.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Falador");
			sleepUntil(() -> faladorparktp.contains(getLocalPlayer()), randomNum(1500,1700));
			sleep(randomNum(932,1340));
		}

		if (faladorparkbig.contains(getLocalPlayer()) && !faladorparkbridge.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(faladorparkbridge.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (faladorparkbridge.contains(getLocalPlayer()) && faladorparkbig.contains(getLocalPlayer())) {
			NPC cecilia = NPCs.closest("Cecilia"); 
			if (cecilia != null) {
				cecilia.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(3500, 4500));
				sleep(randomNum(123,411));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.getOptionIndexContaining("Yes, I have.") != -1, randomNum(3500, 4500));
				sleep(randomNum(115,432));
				Dialogues.chooseOption(1);
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
			}
			
			if (Tabs.getOpen() != Tab.MUSIC) {
				Tabs.open(Tab.MUSIC);
				sleep(randomNum(200,400));
			}
			
			int scrollx = Widgets.getWidget(239).getChild(4).getChild(0).getX();
			int scrolly = Widgets.getWidget(239).getChild(4).getChild(0).getY();
			int scrollheight = Widgets.getWidget(239).getChild(4).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(239).getChild(4).getChild(0).getWidth();
			//Mouse.click(new Point(randomNum(scrollx+(scrollwidth/2)-1, scrollx+(scrollwidth/2)+1), randomNum((scrolly+(scrollheight/8)-1),(scrolly+(scrollheight/8)+1))));
			Mouse.click(new Point(scrollx+(scrollwidth/2), scrolly+(scrollheight/8)));
			sleep(randomNum(135,442));
			Widgets.getWidget(239).getChild(3).getChild(276).interact("Play");

			if (cecilia != null) {
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(15, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3607() {
		currentClue = 3607;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !burthorpetoinntodunstan.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> burthorpeteleport.contains(getLocalPlayer()), randomNum(3500,5000));
			sleep(randomNum(520, 800));
		}

		if (!Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(4,6)) && burthorpetoinntodunstan.contains(getLocalPlayer()) && !burthorpeinn.contains(getLocalPlayer())) {
			
			if (Combat.isAutoRetaliateOn() == false) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(true);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == true, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(burthorpeinnsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Key (medium)") && burthorpeinn.contains(getLocalPlayer())) {
			if (getLocalPlayer().isInCombat()) {
				if (getLocalPlayer().getHealthPercent() <= 50 && Inventory.contains("Shark")) {
					Inventory.interact("Shark", "Eat");
					sleep(randomNum(200, 400));
				}
			} else if (!getLocalPlayer().isInCombat()) {
				sleepUntil(() -> getLocalPlayer().isInCombat(), randomNum(3000, 4000));
				GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
				if (mediumkey == null) {
					NPC penda = NPCs.closest(f -> f != null && f.getName().contentEquals("Penda")); 
					if (penda != null && Map.canReach(penda)) {
						penda.interact("Attack");
						randomNum(600, 800);
					}
				}
			}

			GroundItem mediumkey = GroundItems.closest(f -> f.getName().contentEquals("Key (medium)") && Map.canReach(f));
			if (mediumkey != null && mediumkey.isOnScreen() && !getLocalPlayer().isMoving()) {
				mediumkey.interact("Take");
				sleepUntil(() -> Inventory.contains("Key (medium)"), randomNum(5000, 7000));
				sleep(randomNum(100, 200));
			}
		}
		
		if (Inventory.contains("Key (medium)") && Walking.shouldWalk(randomNum(4,6)) && burthorpetoinntodunstan.contains(getLocalPlayer()) && !dunstanshouseroom.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}

			Walking.walk(dunstanshouseroomsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (Inventory.contains("Key (medium)") && dunstanshouseroom.contains(getLocalPlayer())) {
			GameObject drawers = GameObjects.closest(f -> f.getName().contentEquals("Drawers") && dunstanshouseroom.contains(f));
			if (drawers != null) {
				drawers.interact("Open");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 20) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
				
				if (Combat.isAutoRetaliateOn() == true) {
					sleep(randomNum(100,300));
			        Combat.toggleAutoRetaliate(false);
			        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
					sleep(randomNum(100,300));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2811() {
		currentClue = 2811;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !barbarianoutposttobaxtorian.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int gamesneck = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(gamesneck, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(2);
			sleepUntil(() -> barbarianoutpostteleport.contains(getLocalPlayer()), randomNum(1500,2000));
			sleep(randomNum(820, 950));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && barbarianoutposttobaxtorian.contains(getLocalPlayer()) && !baxtorianisland2.contains(getLocalPlayer()) && !baxtorianisland1.contains(getLocalPlayer()) && !raftarea.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(raftareasmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (raftarea.contains(getLocalPlayer())) {
			GameObject raft = GameObjects.closest("Log raft");
			if (raft != null) {
				raft.interact("Board");
				sleepUntil(() -> baxtorianisland1.contains(getLocalPlayer()), randomNum(6300, 8500));
				sleep(randomNum(150, 300));
			}
		} 
		
		if (baxtorianisland1.contains(getLocalPlayer()) && Inventory.contains("Rope")) {
			GameObject rock = GameObjects.closest(f -> f.getName().contentEquals("Rock") && rockareabaxtorianfalls.contains(f));
			if (rock != null) {
				sleep(randomNum(650, 800));
				Inventory.interact("Rope", "Use");
				sleepUntil(() -> Inventory.isItemSelected(), randomNum(6300, 8500));
				sleep(randomNum(150, 300));
				rock.interact("Use");
				sleepUntil(() -> baxtorianisland2.contains(getLocalPlayer()), randomNum(6300, 8500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (baxtorianisland2.contains(getLocalPlayer()) && !c2811digtile.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(1,2))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c2811digtile.getCenter());
			sleep(randomNum(300, 500));
		}
		
		if (baxtorianisland2.contains(getLocalPlayer()) && c2811digtile.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2858() {
		currentClue = 2858;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !karamjatoluthas.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(460, 700));
			Dialogues.chooseOption(2);
			sleepUntil(() -> karamjateleport.contains(getLocalPlayer()), randomNum(4500,6000));
			sleep(randomNum(320, 700));
		}
		
		if (karamjatoluthas.contains(getLocalPlayer()) && !luthashouse.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(luthashousesmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (luthashouse.contains(getLocalPlayer())) {
			NPC luthas = NPCs.closest("Luthas"); 
			if (luthas != null) {
				luthas.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
				sleep(randomNum(200, 400));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(50, 80));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7317() {
		currentClue = 7317;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!fairyrintoshilovines.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!fairyrintoshilovines.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SOUTH_TAI_BWO_WANNAI_VILLAGE);
					sleepUntil(() -> CKRteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (fairyrintoshilovines.contains(getLocalPlayer()) && !shilovinesdigspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(5,7))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walkExact(shilovinesdigspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (fairyrintoshilovines.contains(getLocalPlayer()) && shilovinesdigspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19762() {
		currentClue = 19762;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!arceuustolibrary.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!arceuustolibrary.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.ARCEUUS_LIBRARY);
					sleepUntil(() -> arceuuslibraryteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (arceuustolibrary.contains(getLocalPlayer()) && !arceuuslibrarygrackle.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(arceuuslibrarygracklesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
	
		if (arceuuslibrarygrackle.contains(getLocalPlayer())) {
			NPC grackle = NPCs.closest("Professor Gracklebone"); 
			if (grackle != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					grackle.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("9");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					grackle.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19748() {
		currentClue = 19748;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !burthorpetodunstan.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> burthorpeteleport.contains(getLocalPlayer()), randomNum(3500,5000));
			sleep(randomNum(520, 800));
		}

		if (Walking.shouldWalk(randomNum(4,6)) && burthorpetodunstan.contains(getLocalPlayer()) && !dunstanshouselarge.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(dunstanshouselargesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (burthorpetodunstan.contains(getLocalPlayer()) && dunstanshouselarge.contains(getLocalPlayer())) {
			NPC dunstan = NPCs.closest("Dunstan"); 
			if (dunstan != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					dunstan.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("8");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					dunstan.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23138() {
		currentClue = 23138;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
	            Combat.toggleAutoRetaliate(false);
	            sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !faladorparkbig.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Falador");
			sleepUntil(() -> faladorparktp.contains(getLocalPlayer()), randomNum(1500,1700));
			sleep(randomNum(932,1240));
		}

		if (faladorparkbig.contains(getLocalPlayer()) && !faladorparkbridge.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(faladorparkbridge.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (faladorparkbridge.contains(getLocalPlayer()) && faladorparkbig.contains(getLocalPlayer())) {
			NPC cecilia = NPCs.closest("Cecilia"); 
			if (cecilia != null) {
				cecilia.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(3500, 4500));
				sleep(randomNum(123,411));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.getOptionIndexContaining("Yes, I have.") != -1, randomNum(3500, 4500));
				sleep(randomNum(115,432));
				Dialogues.chooseOption(1);
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
			}
			
			if (Tabs.getOpen() != Tab.MUSIC) {
				Tabs.open(Tab.MUSIC);
				sleep(randomNum(200,400));
			}

			int scrollx = Widgets.getWidget(239).getChild(4).getChild(0).getX();
			int scrolly = Widgets.getWidget(239).getChild(4).getChild(0).getY();
			int scrollheight = Widgets.getWidget(239).getChild(4).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(239).getChild(4).getChild(0).getWidth();
			//Mouse.click(new Point(randomNum(scrollx+(scrollwidth/2)-1, scrollx+(scrollwidth/2)+1), randomNum((int) (scrolly+(scrollheight/2.17)-1), (int) (scrolly+(scrollheight/2.17)+1))));
			Mouse.click(new Point(scrollx+(scrollwidth/2), (int) (scrolly+(scrollheight/2.17))));
			sleep(randomNum(135,442));
			Widgets.getWidget(239).getChild(3).getChild(262).interact("Play");

			if (cecilia != null) {
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(15, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7294() {
		currentClue = 7294;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!cjrto7294digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (!cjrto7294digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.SINCLAIR_MANSION);
					sleepUntil(() -> cjrteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (cjrto7294digspot.contains(getLocalPlayer()) && !c7294digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c7294digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (cjrto7294digspot.contains(getLocalPlayer()) && c7294digspot.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12047() {
		currentClue = 12047;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!piscatoristo12047digtile.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!piscatoristo12047digtile.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.KANDARIN_PISCATORIS);
					sleepUntil(() -> kandarinpiscatoristp.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (piscatoristo12047digtile.contains(getLocalPlayer()) && !c12047digtile.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c12047digtile.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (piscatoristo12047digtile.contains(getLocalPlayer()) && c12047digtile.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2825() {
		currentClue = 2825;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!draynormanorto2825digspot.contains(getLocalPlayer())) {
			if (Inventory.contains("Draynor manor teleport")) {
				Inventory.interact("Draynor manor teleport", "Break");
				sleepUntil(() -> draynormanortp.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (draynormanorto2825digspot.contains(getLocalPlayer()) && !digspot2825.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(digspot2825.getCenter());
			sleep(randomNum(200,400));
		}
		
		if (draynormanorto2825digspot.contains(getLocalPlayer()) && digspot2825.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23133() {
		currentClue = 23133;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !grandexchangearea.contains(getLocalPlayer()) && !corsaircovetospa.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Grand Exchange");
			sleepUntil(() -> grandexchangearea.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}

		if (grandexchangearea.contains(getLocalPlayer()) && !corsaircovetospa.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(6,9))) {
			GameObject spirittree = GameObjects.closest(f -> f.getName().contentEquals("Spirit tree") && f.hasAction("Travel"));
			if (spirittree != null & gespirittreelarge.contains(getLocalPlayer())) {
				spirittree.interact("Travel");
				sleepUntil(() -> spirittreesmall.contains(getLocalPlayer()) && !getLocalPlayer().isMoving() && !getLocalPlayer().isAnimating(), randomNum(8500, 10500));
				sleep(randomNum(732,1040));
				Keyboard.type("5");
				sleepUntil(() -> corsaircovespirittree.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(gespirittree.getRandomTile());
			sleep(randomNum(232,540));
		}

		if (Walking.shouldWalk(randomNum(6,9)) && !spaladybig.contains(getLocalPlayer()) && corsaircovetospa.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			

			Walking.walk(spaladysmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (spaladybig.contains(getLocalPlayer()) && corsaircovetospa.contains(getLocalPlayer())) {
			NPC madame = NPCs.closest("Madame Caldarium"); 
			if (madame != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					madame.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("6");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					madame.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
		
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C19772() {
		currentClue = 19772;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}

		if (!drezelcaveinside.contains(getLocalPlayer()) && !CKStodrezelcave.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!drezelcaveinside.contains(getLocalPlayer()) && !CKStodrezelcave.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.CANIFIS);
					sleepUntil(() -> CKSteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (!drezelcaveinside.contains(getLocalPlayer()) && CKStodrezelcave.contains(getLocalPlayer()) && !drezelcaveentrance.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(drezelcaveentrancesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!drezelcaveinside.contains(getLocalPlayer()) && drezelcaveentrance.contains(getLocalPlayer())) {
			GameObject trapdoor = GameObjects.closest("Trapdoor");
			if (trapdoor != null && trapdoor.hasAction("Open")) {
				trapdoor.interact("Open");
				sleepUntil(() -> tilebeforetrapdoor.contains(getLocalPlayer()), randomNum(5300, 7500));
				sleep(randomNum(150, 300));
			} else if (trapdoor != null && trapdoor.hasAction("Climb-down")) {
				trapdoor.interact("Climb-down");
				sleepUntil(() -> drezelcaveinside.contains(getLocalPlayer()), randomNum(5300, 7500));
				sleep(randomNum(150, 300));
			}
		}
		
		if (drezelcaveinside.contains(getLocalPlayer()) && !drezelarea.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			Walking.walk(drezelareasmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (drezelarea.contains(getLocalPlayer())) {
			NPC drezel = NPCs.closest("Drezel"); 
			if (drezel != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					drezel.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(380, 550));
					Keyboard.type("7");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					drezel.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(6500, 7400));
					while (!Inventory.contains("Challenge scroll (medium)") && Dialogues.inDialogue()) {
						sleepUntil(() -> Dialogues.canContinue(), randomNum(2000, 3400));
						sleep(randomNum(200, 400));
						Dialogues.continueDialogue();
						sleep(randomNum(200, 400));
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23141() {
		currentClue = 23141;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Equipment.contains(f -> f != null && f.getName().contains("Ring of wealth (")) && !faladorparkbig.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.RING, "Falador");
			sleepUntil(() -> faladorparktp.contains(getLocalPlayer()), randomNum(1500,1700));
			sleep(randomNum(932,1340));
		}

		if (faladorparkbig.contains(getLocalPlayer()) && !faladorparkbridge.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(faladorparkbridge.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (faladorparkbridge.contains(getLocalPlayer()) && faladorparkbig.contains(getLocalPlayer())) {
			NPC cecilia = NPCs.closest("Cecilia"); 
			if (cecilia != null) {
				cecilia.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(3500, 4500));
				sleep(randomNum(123,411));
				Dialogues.continueDialogue();
				sleepUntil(() -> Dialogues.getOptionIndexContaining("Yes, I have.") != -1, randomNum(3500, 4500));
				sleep(randomNum(115,432));
				Dialogues.chooseOption(1);
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
			}
			
			if (Tabs.getOpen() != Tab.MUSIC) {
				Tabs.open(Tab.MUSIC);
				sleep(randomNum(200,400));
			}
			
			int scrollx = Widgets.getWidget(239).getChild(4).getChild(0).getX();
			int scrolly = Widgets.getWidget(239).getChild(4).getChild(0).getY();
			int scrollheight = Widgets.getWidget(239).getChild(4).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(239).getChild(4).getChild(0).getWidth();
			//Mouse.click(new Point(randomNum(scrollx+(scrollwidth/2)-1, scrollx+(scrollwidth/2)+1), randomNum((scrolly+(scrollheight/8))-1, (scrolly+(scrollheight/8))+1)));
			Mouse.click(new Point(scrollx+(scrollwidth/2), scrolly+(scrollheight/8)));
			sleep(randomNum(135,442));
			Widgets.getWidget(239).getChild(3).getChild(344).interact("Play");

			if (cecilia != null) {
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(135,442));
				Dialogues.continueDialogue();
				sleep(randomNum(135,442));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(3500, 4500));
				sleep(randomNum(146,421));
				Dialogues.continueDialogue();
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,220));
				}
				sleep(randomNum(15, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3610() {
		currentClue = 3610;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!toweroflifetoardougnemonastery.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!toweroflifetoardougnemonastery.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.TOWER_OF_LIFE);
					sleepUntil(() -> toweroflifetoardougnemonastery.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}

		if (!crateinardougnemonasterybig.contains(getLocalPlayer()) && toweroflifetoardougnemonastery.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(crateinardougnemonasterysmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (crateinardougnemonasterybig.contains(getLocalPlayer())) {
			GameObject crate = GameObjects.closest(f -> f.getName().contentEquals("Crate") && crateardougnemonastery.contains(f));
			if (crate != null) {
				crate.interact("Search");
				int i = 0;
				while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
					i++;
					sleep(randomNum(180,230));
				}
				sleep(randomNum(10, 30));
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2813() {
		currentClue = 2813;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!toweroflifeto2813digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!toweroflifeto2813digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.TOWER_OF_LIFE);
					sleepUntil(() -> toweroflifeto2813digspot.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}

		if (!c2813digspot.contains(getLocalPlayer()) && toweroflifeto2813digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c2813digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (c2813digspot.contains(getLocalPlayer()) && toweroflifeto2813digspot.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10268() {
		currentClue = 10268;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!yanilletobank.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!yanilletobank.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.NW_YANILLE);
					sleepUntil(() -> nwayanilletp.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (yanilletobank.contains(getLocalPlayer()) && !yanilleinfrontofbank.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			GameObject largedoor2 = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && yanillefirstdoor.contains(f));
			if (largedoor2 != null && largedoor2.getTile().distance() <= 10 && yanilleoutside.contains(getLocalPlayer())) {
				largedoor2.interact("Open");
				sleepUntil(() -> infrontoflargedoor2.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			GameObject largedoor = GameObjects.closest(f -> f.hasAction("Open") && f.getName().contentEquals("Large door") && yanilleseconddoor.contains(f));
			if (largedoor != null && largedoor.getTile().distance() <= 10 && yanilleairlock.contains(getLocalPlayer())) {
				largedoor.interact("Open");
				sleepUntil(() -> infrontoflargedoor.contains(getLocalPlayer()), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			Walking.walk(yanilleinfrontofbanksmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Adamant med helm") && !Equipment.contains("Adamant med helm") && yanilleinfrontofbank.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Adamant med helm") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		if (Inventory.contains("Adamant med helm") && !Equipment.contains("Adamant med helm") && yanilleinfrontofbank.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Snakeskin chaps", "Wear");
			sleepUntil(() -> Equipment.contains("Snakeskin chaps"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Brown apron", "Wear");
			sleepUntil(() -> Equipment.contains("Brown apron"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Adamant med helm", "Wear");
			sleepUntil(() -> Equipment.contains("Adamant med helm"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Adamant med helm") && Equipment.contains("Adamant med helm") && yanilletobank.contains(getLocalPlayer()) && !yanilleinsidebank.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
	    	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(yanilleinsidebanksmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Adamant med helm") && Equipment.contains("Adamant med helm") && yanilleinsidebank.contains(getLocalPlayer())) {
			sleep(randomNum(60, 150));
			if(Tabs.getOpen() != Tab.EMOTES) {
				Tabs.open(Tab.EMOTES);
				sleep(randomNum(73,212));
			}
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.JUMP_FOR_JOY);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.JIG);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Adamant med helm") && Equipment.contains("Adamant med helm")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Adamant med helm") || Equipment.contains("Adamant med helm")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					while (Equipment.contains("Adamant med helm")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> !Equipment.contains("Snakeskin chaps"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> !Equipment.contains("Brown apron"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> !Equipment.contains("Adamant med helm"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
	
					while (Inventory.contains("Adamant med helm") && !Equipment.contains("Adamant med helm") && yanilletobank.contains(getLocalPlayer()) && !yanilleinfrontofbank.contains(getLocalPlayer())) {
						Walking.walk(yanilleinfrontofbanksmall.getRandomTile());
						sleep(randomNum(500, 800));
						sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(4500, 6500));
						sleep(randomNum(100, 300));
					}
					
					while (Inventory.contains("Adamant med helm") && !Equipment.contains("Adamant med helm") && yanilletobank.contains(getLocalPlayer()) && yanilleinfrontofbank.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Adamant med helm") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23135() {
		currentClue = 23135;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Skills necklace(")) && !farmingguild.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int skillsneck = Inventory.slot(f -> f.getName().contains("Skills necklace("));
			Inventory.slotInteract(skillsneck, "Rub");
			sleep(randomNum(845,930));
			Widgets.getWidget(187).getChild(3).getChild(5).interact("Continue");
			sleepUntil(() -> farmingguildtp.contains(getLocalPlayer()), randomNum(3500,4800));
			sleep(randomNum(745,900));
		}
		
		if (farmingguild.contains(getLocalPlayer()) && !c23135digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(2,3))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c23135digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (farmingguild.contains(getLocalPlayer()) && c23135digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12031() {
		currentClue = 12031;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !edgevilletogeneralstore.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> edgevillecentre.contains(getLocalPlayer()), randomNum(4500,8000));
			sleep(randomNum(320, 600));
		}
		
		if (!Inventory.contains("Leather gloves") && !Equipment.contains("Leather gloves") && edgevilletogeneralstore.contains(getLocalPlayer()) && !edgevillestashunit.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(2,3))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(edgevillestashunitsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Leather gloves") && !Equipment.contains("Leather gloves") && edgevillestashunit.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Leather gloves") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		if (Inventory.contains("Leather gloves") && !Equipment.contains("Leather gloves") && edgevillestashunit.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Brown apron", "Wear");
			sleepUntil(() -> Equipment.contains("Brown apron"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Leather boots", "Wear");
			sleepUntil(() -> Equipment.contains("Leather boots"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Leather gloves", "Wear");
			sleepUntil(() -> Equipment.contains("Leather gloves"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Leather gloves") && Equipment.contains("Leather gloves") && edgevilletogeneralstore.contains(getLocalPlayer()) && !edgevillegeneralstore.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
	    	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(edgevillegeneralstoresmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Leather gloves") && Equipment.contains("Leather gloves") && edgevillegeneralstore.contains(getLocalPlayer())) {
			sleep(randomNum(60, 150));
			if(Tabs.getOpen() != Tab.EMOTES) {
				Tabs.open(Tab.EMOTES);
				sleep(randomNum(73,212));
			}
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.CHEER);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.DANCE);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Leather gloves") && Equipment.contains("Leather gloves")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Leather gloves") || Equipment.contains("Leather gloves")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					while (Equipment.contains("Leather gloves")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> !Equipment.contains("Brown apron"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Boots of lightness", "Wear");
						sleepUntil(() -> !Equipment.contains("Leather boots"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int combatbracelet = Inventory.slot(f -> f.getName().contains("Combat bracelet"));
						Inventory.slotInteract(combatbracelet, "Wear");
						sleepUntil(() -> !Equipment.contains("Leather gloves"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
	
					while (Inventory.contains("Leather gloves") && !Equipment.contains("Leather gloves") && edgevilletogeneralstore.contains(getLocalPlayer()) && !edgevillestashunit.contains(getLocalPlayer())) {
						Walking.walk(edgevillestashunitsmall.getRandomTile());
						sleep(randomNum(500, 800));
						sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(4500, 6500));
						sleep(randomNum(100, 300));
					}
					
					while (Inventory.contains("Leather gloves") && !Equipment.contains("Leather gloves") && edgevilletogeneralstore.contains(getLocalPlayer()) && edgevillestashunit.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Leather gloves") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10258() {
		currentClue = 10258;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Amulet of glory(")) && !edgevilletobarbbridge.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int glory = Inventory.slot(f -> f.getName().contains("Amulet of glory("));
			Inventory.slotInteract(glory, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(1);
			sleepUntil(() -> edgevillecentre.contains(getLocalPlayer()), randomNum(4500,8000));
			sleep(randomNum(320, 600));
		}
		
		if (!Inventory.contains("Purple gloves") && !Equipment.contains("Purple gloves") && edgevilletobarbbridge.contains(getLocalPlayer()) && !barbbridgestash.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(barbbridgestashsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Purple gloves") && !Equipment.contains("Purple gloves") && barbbridgestash.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Purple gloves") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		if (Inventory.contains("Purple gloves") && !Equipment.contains("Purple gloves") && barbbridgestash.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Mithril full helm", "Wear");
			sleepUntil(() -> Equipment.contains("Mithril full helm"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Steel kiteshield", "Wear");
			sleepUntil(() -> Equipment.contains("Steel kiteshield"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Purple gloves", "Wear");
			sleepUntil(() -> Equipment.contains("Purple gloves"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Purple gloves") && Equipment.contains("Purple gloves") && edgevilletobarbbridge.contains(getLocalPlayer()) && !barbbridge.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
	    	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(barbbridgesmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Purple gloves") && Equipment.contains("Purple gloves") && barbbridge.contains(getLocalPlayer())) {
			sleep(randomNum(60, 150));
			if(Tabs.getOpen() != Tab.EMOTES) {
				Tabs.open(Tab.EMOTES);
				sleep(randomNum(73,212));
			}
			Emotes.doEmote(Emote.SPIN);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));

			int scrollx = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx+(scrollwidth/2)+randomNum(1,4), scrolly+(scrollheight/2)+randomNum(1,4)));
			sleep(randomNum(60, 150));

			Widgets.getWidget(216).getChild(1).getChild(21).interact("Salute");
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			int scrollx1 = Widgets.getWidget(216).getChild(2).getChild(0).getX();
			int scrolly1 = Widgets.getWidget(216).getChild(2).getChild(0).getY();
			int scrollheight1 = Widgets.getWidget(216).getChild(2).getChild(0).getHeight();
			int scrollwidth1 = Widgets.getWidget(216).getChild(2).getChild(0).getWidth();
			Mouse.click(new Point(scrollx1+(scrollwidth1/2)+randomNum(1,4), scrolly1+(scrollheight1/8)+randomNum(1,4)));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Purple gloves") && Equipment.contains("Purple gloves")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Purple gloves") || Equipment.contains("Purple gloves")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					while (Equipment.contains("Purple gloves")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> !Equipment.contains("Mithril full helm"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> !Equipment.contains("Steel kiteshield"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						int combatbracelet = Inventory.slot(f -> f.getName().contains("Combat bracelet"));
						Inventory.slotInteract(combatbracelet, "Wear");
						sleepUntil(() -> !Equipment.contains("Purple gloves"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
	
					while (Inventory.contains("Purple gloves") && !Equipment.contains("Purple gloves") && edgevilletobarbbridge.contains(getLocalPlayer()) && !barbbridgestash.contains(getLocalPlayer())) {
						Walking.walk(barbbridgestashsmall.getRandomTile());
						sleep(randomNum(500, 800));
						sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(4500, 6500));
						sleep(randomNum(100, 300));
					}
					
					while (Inventory.contains("Purple gloves") && !Equipment.contains("Purple gloves") && edgevilletobarbbridge.contains(getLocalPlayer()) && barbbridgestash.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Purple gloves") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C3599() {
		currentClue = 3599;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!toweroflifeto3599digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!toweroflifeto3599digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.TOWER_OF_LIFE);
					sleepUntil(() -> toweroflifeto3599digspot.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}

		if (!c3599digspot.contains(getLocalPlayer()) && toweroflifeto3599digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c3599digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (c3599digspot.contains(getLocalPlayer()) && toweroflifeto3599digspot.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C23137() {
		currentClue = 23137;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!digsitetoboat.contains(getLocalPlayer()) && !southfossilisland.contains(getLocalPlayer()) && !northfossilisland.contains(getLocalPlayer()) && !outatseafossilisland.contains(getLocalPlayer())) {
			if (Inventory.contains("Digsite teleport")) {
				Inventory.interact("Digsite teleport", "Teleport");
				sleepUntil(() -> digsiteteleport.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (digsitetoboat.contains(getLocalPlayer()) && !digsiteboat.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(digsiteboatsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (digsiteboat.contains(getLocalPlayer())) {
			NPC bagguard = NPCs.closest("Barge guard"); 
			if (bagguard != null) {
				bagguard.interact("Quick-Travel");
				sleepUntil(() -> southfossilisland.contains(getLocalPlayer()), randomNum(6500, 8500));
				sleep(randomNum(400,700));
			}
		}
		
		if (southfossilisland.contains(getLocalPlayer())) {
			GameObject rowboatsouth = GameObjects.closest("Rowboat");
			if (rowboatsouth != null) {
				rowboatsouth.interact("Travel");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5300, 6500));
				sleep(randomNum(150, 300));
				Keyboard.type("2");
				sleepUntil(() -> northfossilisland.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
				
			}
		}
		
		if (northfossilisland.contains(getLocalPlayer())) {
			GameObject rowboatnorth = GameObjects.closest("Rowboat");
			if (rowboatnorth != null) {
				rowboatnorth.interact("Travel");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(5300, 6500));
				sleep(randomNum(150, 300));
				Keyboard.type("3");
				sleepUntil(() -> outatseafossilisland.contains(getLocalPlayer()), randomNum(4300, 6300));
				sleep(randomNum(432,740));
				
			}
		}
		
		if (outatseafossilisland.contains(getLocalPlayer()) && !c23137digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(1,2))) {
			Walking.walkExact(c23137digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (outatseafossilisland.contains(getLocalPlayer()) && c23137digspot.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C7311() {
		currentClue = 7311;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!monasterytomountain.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Combat bracelet("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.HANDS, "Monastery");
			sleepUntil(() -> monasterytp.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (monasterytomountain.contains(getLocalPlayer()) && !c7311digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c7311digspot.getCenter());
			sleep(randomNum(232,540));
		}
		
		if (monasterytomountain.contains(getLocalPlayer()) && c7311digspot.contains(getLocalPlayer())) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C2819() {
		currentClue = 2819;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!mudskipperto2819digspot.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!mudskipperto2819digspot.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.MUDSKIPPER_POINT);
					sleepUntil(() -> mudskipperteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (mudskipperto2819digspot.contains(getLocalPlayer()) && !c2819digspot.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walkExact(c2819digspot.getCenter());
			sleep(randomNum(200, 400));
		}
		
		if (mudskipperto2819digspot.contains(getLocalPlayer()) && c2819digspot.contains(getLocalPlayer()) && Inventory.contains("Spade")) {
			Inventory.interact("Spade", "Dig");
			int i = 0;
			while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
				i++;
				sleep(randomNum(180,220));
			}				
			sleep(randomNum(10, 25));
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C12055() {
		currentClue = 12055;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (Inventory.contains(f -> f.getName().contains("Games necklace(")) && !barbarianoutposttoottosgrotto.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			int gamesneck = Inventory.slot(f -> f.getName().contains("Games necklace("));
			Inventory.slotInteract(gamesneck, "Rub");
			sleepUntil(() -> Dialogues.inDialogue(), randomNum(1500,2000));
			sleep(randomNum(60, 300));
			Dialogues.chooseOption(2);
			sleepUntil(() -> barbarianoutpostteleport.contains(getLocalPlayer()), randomNum(1500,2000));
			sleep(randomNum(720, 900));
		}
		
		if (Walking.shouldWalk(randomNum(4,6)) && barbarianoutposttoottosgrotto.contains(getLocalPlayer()) && !ottosgrotto.contains(getLocalPlayer())) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(ottosgrottosmall.getRandomTile());
			sleep(randomNum(300, 500));
		}
		
		if (ottosgrotto.contains(getLocalPlayer())) {
			NPC otto = NPCs.closest("Otto Godblessed"); 
			if (otto != null) {
				if (Inventory.contains("Challenge scroll (medium)")) {
					otto.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleep(randomNum(200, 400));
					sleepUntil(() -> Dialogues.canEnterInput(), randomNum(5500, 6400));
					sleep(randomNum(180, 250));
					Keyboard.type("2");
					sleepUntil(() -> Dialogues.canContinue(), randomNum(2500, 4000));
					sleep(randomNum(180, 250));
					Dialogues.continueDialogue();
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(50, 80));
				} else if (!Inventory.contains("Challenge scroll (medium)")) {
					otto.interact("Talk-to");
					sleepUntil(() -> Dialogues.inDialogue(), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
					Dialogues.continueDialogue();
					sleepUntil(() -> Inventory.contains("Challenge scroll (medium)"), randomNum(5500, 6400));
					sleep(randomNum(200, 400));
				}
			}
		}
			
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10274() {
		currentClue = 10274;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!digsitetoemoteclue.contains(getLocalPlayer())) {
			if (Inventory.contains("Digsite teleport")) {
				Inventory.interact("Digsite teleport", "Teleport");
				sleepUntil(() -> digsiteteleport.contains(getLocalPlayer()), randomNum(3000,4000));
				sleep(randomNum(200,400));
			}
		}
		
		if (!Inventory.contains("Iron pickaxe") && !Equipment.contains("Iron pickaxe") && digsitetoemoteclue.contains(getLocalPlayer()) && !digsitestash.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(4,6))) {
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(digsitestashsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Iron pickaxe") && !Equipment.contains("Iron pickaxe") && digsitestash.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Iron pickaxe") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		if (Inventory.contains("Iron pickaxe") && !Equipment.contains("Iron pickaxe") && digsitestash.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Snakeskin boots", "Wear");
			sleepUntil(() -> Equipment.contains("Snakeskin boots"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Green hat", "Wear");
			sleepUntil(() -> Equipment.contains("Green hat"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Iron pickaxe", "Wield");
			sleepUntil(() -> Equipment.contains("Iron pickaxe"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Iron pickaxe") && Equipment.contains("Iron pickaxe") && digsitetoemoteclue.contains(getLocalPlayer()) && !digsitewell.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5))) {
	    	if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			Walking.walk(digsitewellsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Iron pickaxe") && Equipment.contains("Iron pickaxe") && digsitewell.contains(getLocalPlayer())) {
			sleep(randomNum(60, 150));
			if(Tabs.getOpen() != Tab.EMOTES) {
				Tabs.open(Tab.EMOTES);
				sleep(randomNum(73,212));
			}
			Emotes.doEmote(Emote.BECKON);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.BOW);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Iron pickaxe") && Equipment.contains("Iron pickaxe")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				while (Inventory.contains("Iron pickaxe") || Equipment.contains("Iron pickaxe")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					while (Equipment.contains("Iron pickaxe")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.interact("Boots of lightness", "Wear");
						sleepUntil(() -> !Equipment.contains("Snakeskin boots"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Kandarin headgear 1", "Wear");
						sleepUntil(() -> !Equipment.contains("Green hat"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> !Equipment.contains("Iron pickaxe"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
	
					while (Inventory.contains("Iron pickaxe") && !Equipment.contains("Iron pickaxe") && digsitetoemoteclue.contains(getLocalPlayer()) && !digsitestash.contains(getLocalPlayer())) {
						Walking.walk(digsitestashsmall.getRandomTile());
						sleep(randomNum(500, 800));
						sleepUntil(() -> !getLocalPlayer().isAnimating() && !getLocalPlayer().isMoving(), randomNum(4500, 6500));
						sleep(randomNum(100, 300));
					}
					
					while (Inventory.contains("Iron pickaxe") && !Equipment.contains("Iron pickaxe") && digsitetoemoteclue.contains(getLocalPlayer()) && digsitestash.contains(getLocalPlayer())) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Iron pickaxe") && !getLocalPlayer().isAnimating(), randomNum(6500,8000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
	
	public void C10254() {
		currentClue = 10254;
		currentCluestr = Integer.toString(currentClue);
		
		if (previousClue != currentClue) {
			if (Walking.isRunEnabled() == false) {
				Walking.toggleRun();
			}
			
			if (Combat.isAutoRetaliateOn() == true) {
				sleep(randomNum(100,300));
		        Combat.toggleAutoRetaliate(false);
		        sleepUntil(() -> Combat.isAutoRetaliateOn() == false, randomNum(1000,2000));
				sleep(randomNum(100,300));
			}
			
			dbbotcluestotal ++;
			timeBeganClue = System.currentTimeMillis();
			
			dbbotworld = Client.getCurrentWorld();
			dbbottask = "Clue "+currentCluestr;
			dbbotruntime = ft(System.currentTimeMillis() - this.timeBegan);
			onlineBotUpdate(dbbotname, dbbotworld, dbbottask, dbbotruntime, dbbotrangerboots, dbbotcluestotal, dbbotcluesperhour, dbbotonline);
		}
		
		if (!CKStocanifis.contains(getLocalPlayer()) && !wizardtowerbig.contains(getLocalPlayer()) && Equipment.contains(f -> f != null && f.getName().contains("Necklace of passage("))) {
			if(Tabs.getOpen() != Tab.EQUIPMENT) {
				Tabs.open(Tab.EQUIPMENT);
				sleep(randomNum(73,212));
			}
			
			Equipment.interact(EquipmentSlot.AMULET, "Wizards' Tower");
			sleepUntil(() -> wizardtowerbig.contains(getLocalPlayer()), randomNum(2300, 4500));
			sleep(randomNum(232,540));
		}
		
		if (!CKStocanifis.contains(getLocalPlayer()) && wizardtowerbig.contains(getLocalPlayer()) && Walking.shouldWalk(randomNum(3,5)) ) {
			GameObject fairyring = GameObjects.closest("Fairy ring");
			if (fairyring != null && Map.canReach(fairyring)  && fairyring.distance() <= 8) {
				if (Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff")) {
					Inventory.interact("Dramen staff", "Wield");
					sleepUntil(() -> Equipment.contains("Dramen staff"), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
				fairyring.interact("Configure");
				sleepUntil(() -> FairyRings.travelInterfaceOpen() == true, randomNum(3300, 4200));
				if (FairyRings.travelInterfaceOpen() == true) {
					sleep(randomNum(150, 300));
					FairyRings.travel(FairyLocation.CANIFIS);
					sleepUntil(() -> CKSteleport.contains(getLocalPlayer()), randomNum(4300, 5500));
					sleep(randomNum(150, 300));
				}
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}
			
			if (wizardtowerbig.contains(getLocalPlayer())) {
				Walking.walk(wizardtowerfairyring.getRandomTile());
				sleep(randomNum(200, 400));
			}
		}
		
		if (!Inventory.contains("Iron 2h sword") && Walking.shouldWalk(randomNum(6,9)) && CKStocanifis.contains(getLocalPlayer()) && !canifisstashunit.contains(getLocalPlayer())) {
			
			if (Inventory.contains("Magic shortbow") && !Equipment.contains("Magic shortbow")) {
				Inventory.interact("Magic shortbow", "Wield");
				sleepUntil(() -> !Equipment.contains("Dramen staff"), randomNum(4300, 5500));
				sleep(randomNum(150, 300));
			}
			
			if (Walking.getRunEnergy() <= randomNum(70,80) && (PlayerSettings.getConfig(1575) == 0 || PlayerSettings.getConfig(1575) == 8388608)  && Inventory.contains(f -> f.getName().contains("Stamina potion("))) {
				int stampot = Inventory.slot(stamfilter -> stamfilter.getName().contains("Stamina potion("));
				if (Tabs.getOpen() != Tab.INVENTORY) {
					Tabs.open(Tab.INVENTORY);
					sleep(randomNum(200,400));
				}
				Inventory.slotInteract(stampot, "Drink");
				sleep(randomNum(200,300));
			}

			Walking.walk(canifisstashunitsmall.getRandomTile());
			sleep(randomNum(200, 400));
		}
		
		if (!Inventory.contains("Iron 2h sword") && !Equipment.contains("Iron 2h sword") && canifisstashunit.contains(getLocalPlayer())) {
			GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
			if (STASH != null) {
				STASH.interact("Search");
				sleepUntil(() -> Inventory.contains("Iron 2h sword") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
				sleep(randomNum(500, 800));
			}
		}
		
		if (Inventory.contains("Iron 2h sword") && !Equipment.contains("Iron 2h sword") && canifisstashunit.contains(getLocalPlayer())) {
			if(Tabs.getOpen() != Tab.INVENTORY) {
				Tabs.open(Tab.INVENTORY);
				sleep(randomNum(73,212));
			}
			Inventory.interact("Mithril platelegs", "Wear");
			sleepUntil(() -> Equipment.contains("Mithril platelegs"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Green robe top", "Wear");
			sleepUntil(() -> Equipment.contains("Green robe top"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
			Inventory.interact("Iron 2h sword", "Wield");
			sleepUntil(() -> Equipment.contains("Iron 2h sword"), randomNum(3500,5000));
			sleep(randomNum(100, 200));
		}
		
		if (!Inventory.contains("Iron 2h sword") && Equipment.contains("Iron 2h sword") && canifisstashunit.contains(getLocalPlayer())) {
			Emotes.doEmote(Emote.DANCE);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			Emotes.doEmote(Emote.BOW);
			sleep(randomNum(700, 850));
			sleepUntil(() -> !getLocalPlayer().isAnimating(), randomNum(800,1200));
			sleep(randomNum(60, 150));
			
			NPC uri = NPCs.closest("Uri");
			if (uri == null) {
				sleepUntil(() -> uri != null, randomNum(2300, 3400));
				sleep(randomNum(100,300));
			}
			
			if (uri != null && !Inventory.contains("Iron 2h sword") && Equipment.contains("Iron 2h sword")) {
				uri.interact("Talk-to");
				sleepUntil(() -> Dialogues.inDialogue(), randomNum(2600, 3500));
				sleep(randomNum(300,500));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				sleepUntil(() -> Dialogues.canContinue(), randomNum(2600, 3500));
				sleep(randomNum(100,300));
				Dialogues.continueDialogue();
				sleep(randomNum(300,500));
				while (Equipment.contains("Iron 2h sword") || Inventory.contains("Iron 2h sword")) {
					int i = 0;
					while ((Inventory.contains(currentClue)) && !Inventory.contains("Reward casket (medium)") && i <= 7) {
						i++;
						sleep(randomNum(180,220));
					}
					sleep(randomNum(20,50));
					
					while (Equipment.contains("Iron 2h sword")) {
						if(Tabs.getOpen() != Tab.INVENTORY) {
							Tabs.open(Tab.INVENTORY);
							sleep(randomNum(73,212));
						}
						Inventory.interact("Green d'hide chaps", "Wear");
						sleepUntil(() -> !Equipment.contains("Mithril platelegs"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Green d'hide body", "Wear");
						sleepUntil(() -> !Equipment.contains("Green robe top"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
						Inventory.interact("Magic shortbow", "Wield");
						sleepUntil(() -> !Equipment.contains("Iron 2h sword"), randomNum(3500,5000));
						sleep(randomNum(100, 200));
					}
					
					while (!Equipment.contains("Iron 2h sword") && Inventory.contains("Iron 2h sword")) {
						GameObject STASH = GameObjects.closest(f -> f.getName().contentEquals("STASH (medium)"));
						if (STASH != null) {
							STASH.interact("Search");
							sleepUntil(() -> !Inventory.contains("Iron 2h sword") && !getLocalPlayer().isAnimating(), randomNum(3500,5000));
							sleep(randomNum(500, 800));
						}
					}
				}
			}
		}
		
		if (Inventory.contains("Reward casket (medium)")) {
			setupcomplete = 0;
			sleep(randomNum(300,600));
		} else if ((System.currentTimeMillis() - this.timeBeganClue) >= 300000) {
			log("stuck on clue: " + currentClue);
			if (Inventory.contains("Clue scroll (medium)")) {
				sleep(randomNum(400,700));
				Inventory.drop("Clue scroll (medium)");
				sleep(randomNum(300,600));
			}
			backing = 1;
			setupcomplete = 0;
		}
		
		previousClue = currentClue;
	}
}