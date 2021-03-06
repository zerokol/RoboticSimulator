package com.zerokol.rs;

import java.awt.Font;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.command.BasicCommand;
import org.newdawn.slick.command.Command;
import org.newdawn.slick.command.InputProvider;
import org.newdawn.slick.command.InputProviderListener;
import org.newdawn.slick.command.KeyControl;
import org.newdawn.slick.geom.Circle;
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Rectangle;

import com.zerokol.rs.actors.Robot;

public class RoboticSimulator extends BasicGame implements
		InputProviderListener {

	private GameContainer ggc;

	private InputProvider provider;

	private Command quit = new BasicCommand("quit");

	private Command up = new BasicCommand("up");
	private boolean upPressed = false;
	private Command right = new BasicCommand("right");
	private boolean rightPressed = false;
	private Command down = new BasicCommand("down");
	private boolean downPressed = false;
	private Command left = new BasicCommand("left");
	private boolean leftPressed = false;

	private long elapsedTime = 1;
	private long period = 15;

	private Image background, world, stick, ball;

	private Image blockMap;
	private Image featureMap;

	private Point screenOrigin = new Point(0, 0);
	private Point worldOrigin = new Point(12, 18);
	private Point stickOrigin = new Point(150, 400);
	private Point ballOrigin = new Point(500, 200);
	private Point blockMapOrigin = new Point(823, 16);
	private Point featureMapOrigin = new Point(823, 192);

	private int worldWidth = 800;
	private int worldHeight = 600;

	private Rectangle upWallColl, rightWallColl, downWallColl, leftWallColl,
			stickColl;
	private Circle ballColl;

	private Robot robot;

	private int blockSizeWorld = 20;
	private int blockWSize, blockHSize;
	private int blockMapMatrix[][];

	private int smallMapProportion = 4;

	private int blockMapsSize = blockSizeWorld / smallMapProportion;

	private int featureMapMatrix[][];
	private int featureWSize, featureHSize;

	private Font awtFont;
	private TrueTypeFont fontMedium;

	private boolean updateBlockMap, updateFeatureMap = false;

	public RoboticSimulator(String title) {
		super(title);
	}

	@Override
	public void init(GameContainer gc) throws SlickException {
		this.ggc = gc;

		// gc.setShowFPS(false);

		this.provider = new InputProvider(gc.getInput());
		this.provider.addListener(this);

		this.provider.bindCommand(new KeyControl(Input.KEY_ESCAPE), this.quit);

		this.provider.bindCommand(new KeyControl(Input.KEY_UP), this.up);
		this.provider.bindCommand(new KeyControl(Input.KEY_RIGHT), this.right);
		this.provider.bindCommand(new KeyControl(Input.KEY_DOWN), this.down);
		this.provider.bindCommand(new KeyControl(Input.KEY_LEFT), this.left);

		this.background = new Image("assets/screen.png");

		this.world = new Image("assets/world.png");

		this.stick = new Image("assets/stick.png");
		this.ball = new Image("assets/ball.png");

		this.robot = new Robot("assets/robot.png", 132, 138);

		this.upWallColl = new Rectangle(this.worldOrigin.getX(),
				this.worldOrigin.getY(), worldWidth, 30);

		this.rightWallColl = new Rectangle(this.worldOrigin.getX() + worldWidth
				- 30, this.worldOrigin.getY(), 30, worldHeight);

		this.downWallColl = new Rectangle(this.worldOrigin.getX(),
				this.worldOrigin.getY() + worldHeight - 30, worldWidth, 30);

		this.leftWallColl = new Rectangle(this.worldOrigin.getX(),
				this.worldOrigin.getY(), 30, worldHeight);

		this.stickColl = new Rectangle(this.stickOrigin.getX(),
				this.stickOrigin.getY(), 600, 20);

		this.ballColl = new Circle(this.ballOrigin.getX()
				+ this.ball.getWidth() / 2, this.ballOrigin.getY()
				+ this.ball.getHeight() / 2, this.ball.getWidth() / 2);

		this.blockMap = new Image(worldWidth / smallMapProportion, worldHeight
				/ smallMapProportion);

		this.blockWSize = worldWidth / blockSizeWorld;
		this.blockHSize = worldHeight / blockSizeWorld;

		this.blockMapMatrix = new int[blockWSize][blockHSize];

		for (int i = 0; i < blockWSize; i++) {
			for (int j = 0; j < blockHSize; j++) {
				this.blockMapMatrix[i][j] = 127;
			}
		}

		featureWSize = worldWidth / smallMapProportion;
		featureHSize = worldHeight / smallMapProportion;

		this.featureMap = new Image(featureWSize, featureHSize);

		this.featureMapMatrix = new int[featureWSize][featureHSize];

		for (int i = 0; i < featureWSize; i++) {
			for (int j = 0; j < featureHSize; j++) {
				this.featureMapMatrix[i][j] = 254;
			}
		}

		drawBlockMap();

		drawFeatureMap();

		awtFont = new Font("Times New Roman", Font.BOLD, 30);
		fontMedium = new TrueTypeFont(awtFont, false);
	}

	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException {
		this.background
				.draw(this.screenOrigin.getX(), this.screenOrigin.getY());

		this.world.draw(this.worldOrigin.getX(), this.worldOrigin.getY());

		this.stick.draw(this.stickOrigin.getX(), this.stickOrigin.getY());

		this.ball.draw(this.ballOrigin.getX(), this.ballOrigin.getY());

		this.blockMap.draw(this.blockMapOrigin.getX(),
				this.blockMapOrigin.getY());

		this.featureMap.draw(this.featureMapOrigin.getX(),
				this.featureMapOrigin.getY());

		this.robot.drawActor(g);

		this.fontMedium.drawString(830, 560, "X: "
				+ (this.robot.getX() - this.worldOrigin.getX()), Color.black);
		this.fontMedium.drawString(830, 590, "Y: "
				+ (this.robot.getY() - this.worldOrigin.getY()), Color.black);
	}

	@Override
	public void update(GameContainer gc, int delta) throws SlickException {
		this.elapsedTime += delta;

		if (this.elapsedTime > this.period) {
			this.elapsedTime = 0;

			checkSensor();

			if (this.upPressed) {
				Point p = new Point(this.robot.getX(), this.robot.getY());

				p.setX((float) (this.robot.getX() + Math.cos(Math
						.toRadians(this.robot.getInclination()))));

				p.setY((float) (this.robot.getY() + Math.sin(Math
						.toRadians(this.robot.getInclination()))));

				if (!testCollision(p)
						&& !testCollision(new Point(p.getX()
								+ this.robot.getWidth(), p.getY()
								+ this.robot.getHeight()))) {
					this.robot.setX(p.getX());
					this.robot.setY(p.getY());
				}
			}

			if (this.rightPressed) {
				this.robot.increaseRotation();
			}

			if (this.downPressed) {
				Point p = new Point(this.robot.getX(), this.robot.getY());

				p.setX((float) (this.robot.getX() - Math.cos(Math
						.toRadians(this.robot.getInclination()))));

				p.setY((float) (this.robot.getY() - Math.sin(Math
						.toRadians(this.robot.getInclination()))));

				if (!testCollision(p)
						&& !testCollision(new Point(p.getX()
								+ this.robot.getWidth(), p.getY()
								+ this.robot.getHeight()))) {
					this.robot.setX(p.getX());
					this.robot.setY(p.getY());
				}
			}

			if (this.leftPressed) {
				this.robot.decreaseRotation();
			}
		}
	}

	@Override
	public void controlPressed(Command cmd) {
		if (cmd == this.quit) {
			this.ggc.exit();
		}

		if (cmd == this.up) {
			this.upPressed = true;
		}
		if (cmd == this.right) {
			this.rightPressed = true;
		}
		if (cmd == this.down) {
			this.downPressed = true;
		}
		if (cmd == this.left) {
			this.leftPressed = true;
		}
	}

	@Override
	public void controlReleased(Command cmd) {
		if (cmd == this.up) {
			this.upPressed = false;
		}
		if (cmd == this.right) {
			this.rightPressed = false;
		}
		if (cmd == this.down) {
			this.downPressed = false;
		}
		if (cmd == this.left) {
			this.leftPressed = false;
		}
	}

	@SuppressWarnings("unused")
	private boolean testCollision(Rectangle r) {
		if (this.collisionRectangleToRetangle(r, this.upWallColl)) {
			return true;
		}
		if (this.collisionRectangleToRetangle(r, this.rightWallColl)) {
			return true;
		}
		if (this.collisionRectangleToRetangle(r, this.downWallColl)) {
			return true;
		}
		if (this.collisionRectangleToRetangle(r, this.leftWallColl)) {
			return true;
		}
		if (this.collisionRectangleToRetangle(r, this.stickColl)) {
			return true;
		}
		return false;
	}

	private boolean testCollision(Point p) {
		if (this.collisionPointToRetangle(p, this.upWallColl)) {
			return true;
		}
		if (this.collisionPointToRetangle(p, this.rightWallColl)) {
			return true;
		}
		if (this.collisionPointToRetangle(p, this.downWallColl)) {
			return true;
		}
		if (this.collisionPointToRetangle(p, this.leftWallColl)) {
			return true;
		}
		if (this.collisionPointToRetangle(p, this.stickColl)) {
			return true;
		}
		if (this.collisionPointToCircle(p, this.ballColl)) {
			return true;
		}
		return false;
	}

	private boolean collisionRectangleToRetangle(Rectangle r1, Rectangle r2) {
		if (r1.getMaxY() < r2.getMinY())
			return false;
		if (r1.getMaxX() < r2.getMinX())
			return false;
		if (r1.getMinY() > r2.getMaxY())
			return false;
		if (r1.getMinX() > r2.getMaxX())
			return false;
		return true;
	}

	private boolean collisionPointToRetangle(Point p, Rectangle r) {
		if (p.getX() > r.getMinX() && p.getX() < r.getMaxX()
				&& p.getY() > r.getMinY() && p.getY() < r.getMaxY()) {
			return true;
		}
		return false;
	}

	private boolean collisionPointToCircle(Point p, Circle c) {
		return Math.pow(p.getX() - c.getCenterX(), 2)
				+ Math.pow(p.getY() - c.getCenterY(), 2) < Math.pow(
				c.getRadius(), 2);
	}

	private void drawBlockMap() throws SlickException {
		for (int i = 0; i < blockWSize; i++) {
			for (int j = 0; j < blockHSize; j++) {
				switch (this.blockMapMatrix[i][j]) {
				case 0:
					this.blockMap.getGraphics().setColor(Color.black);
					break;
				case 254:
					this.blockMap.getGraphics().setColor(Color.white);
					break;
				case 127:
				default:
					this.blockMap.getGraphics().setColor(Color.gray);
				}

				this.blockMap.getGraphics().fillRect(blockMapsSize * i,
						blockMapsSize * j, blockMapsSize * i + blockMapsSize,
						blockMapsSize * j + blockMapsSize);
			}
		}
		this.blockMap.getGraphics().flush();
	}

	private void drawFeatureMap() throws SlickException {
		for (int i = 0; i < featureWSize; i++) {
			for (int j = 0; j < featureHSize; j++) {
				switch (this.featureMapMatrix[i][j]) {
				case 0:
					this.featureMap.getGraphics().setColor(Color.black);
					break;
				case 254:
					this.featureMap.getGraphics().setColor(Color.white);
					break;
				case 127:
				default:
					this.featureMap.getGraphics().setColor(Color.gray);
				}

				this.featureMap.getGraphics().fillRect(i, j, i + 1, j + 1);
			}
		}
		this.featureMap.getGraphics().flush();
	}

	private void checkSensor() throws SlickException {
		int inclination = robot.inclination - 30;

		for (int s = 0; s < 5; s++) {
			robot.sensorOrigins.get(s).setX(
					(float) (robot.origin.getX() + robot.robotImage.getWidth()
							/ 2 + (Math.cos(Math
							.toRadians(inclination + s * 15))
							* robot.robotImage.getWidth() / 2)));

			robot.sensorOrigins.get(s).setY(
					(float) (robot.origin.getY() + robot.robotImage.getHeight()
							/ 2 + (Math.sin(Math
							.toRadians(inclination + s * 15))
							* robot.robotImage.getHeight() / 2)));

			for (int t = 1; t < 20; t++) {
				robot.sensorDestinations.get(s).setX(
						(float) (robot.sensorOrigins.get(s).getX() + (Math
								.cos(Math.toRadians(inclination + s * 15)))
								* t
								* 5));

				robot.sensorDestinations.get(s).setY(
						(float) (robot.sensorOrigins.get(s).getY() + (Math
								.sin(Math.toRadians(inclination + s * 15)))
								* t
								* 5));

				int xNew = (int) (Math.ceil(robot.sensorDestinations.get(s)
						.getX()) - this.worldOrigin.getX());

				int yNew = (int) (Math.ceil(robot.sensorDestinations.get(s)
						.getY()) - this.worldOrigin.getY());

				int i = xNew / blockSizeWorld;
				if (i > blockWSize - 1) {
					i = blockWSize - 1;
				} else if (i < 0) {
					i = 0;
				}

				int j = yNew / blockSizeWorld;
				if (j > blockHSize - 1) {
					j = blockHSize - 1;
				} else if (j < 0) {
					j = 0;
				}

				if (testCollision(robot.sensorDestinations.get(s))) {
					if (this.blockMapMatrix[i][j] != 0) {
						this.blockMapMatrix[i][j] = 0;
						updateBlockMap = true;
					}

					int fX = (int) (xNew / smallMapProportion);
					int fY = (int) (yNew / smallMapProportion);

					if (this.featureMapMatrix[fX][fY] != 0) {
						this.featureMapMatrix[fX][fY] = 0;
						updateFeatureMap = true;
					}
					break;
				} else {
					if (this.blockMapMatrix[i][j] != 254) {
						this.blockMapMatrix[i][j] = 254;
						
						
						updateBlockMap = true;
					}
				}
			}
		}

		if (updateBlockMap) {
			drawBlockMap();
			updateBlockMap = false;
		}

		if (updateFeatureMap) {
			drawFeatureMap();
			updateFeatureMap = false;
		}
	}
}
