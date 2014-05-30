package jp.co.spookies.android.simplebowl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Bowling view
 * 
 */
public class BowlingView extends SurfaceView implements SurfaceHolder.Callback,
		Runnable {
	private Thread thread;
	private boolean runFlag;
	private int state;
	private Bitmap bgImage;
	private Bitmap[] ballImages;
	private Bitmap[] pinImages;
	private Bitmap effectImage;
	private Bitmap resultImage;
	private Bitmap[] numberImages;
	private Rect bgRect;
	private Rect pinRect;
	private Rect resultRect;
	private Rect numberRect;
	private Paint paint;

	public static final int STATE_INIT = 0;
	public static final int STATE_THROW = 1;
	public static final int STATE_ROLL = 2;
	public static final int STATE_RESULT = 3;

	private float x, y;
	private int ballIndex = 0;
	private int pinIndex = 0;
	private int count = 0;
	private float speed;
	private int curve;

	// ピンの本数
	private final int[] numPins = new int[] { 0, 1, 3, 6, 8, 10 };

	public BowlingView(Context context) {
		super(context);
		getHolder().addCallback(this);
		state = STATE_INIT;

		paint = new Paint();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		bgImage = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
		// ボールの画像（回転）
		ballImages = new Bitmap[] {
				BitmapFactory
						.decodeResource(getResources(), R.drawable.ball_01),
				BitmapFactory
						.decodeResource(getResources(), R.drawable.ball_02),
				BitmapFactory
						.decodeResource(getResources(), R.drawable.ball_03),
				BitmapFactory
						.decodeResource(getResources(), R.drawable.ball_03) };

		// 倒れたピンの画像
		pinImages = new Bitmap[] {
				BitmapFactory.decodeResource(getResources(), R.drawable.pin_00),
				BitmapFactory.decodeResource(getResources(), R.drawable.pin_01),
				BitmapFactory.decodeResource(getResources(), R.drawable.pin_03),
				BitmapFactory.decodeResource(getResources(), R.drawable.pin_06),
				BitmapFactory.decodeResource(getResources(), R.drawable.pin_08),
				BitmapFactory.decodeResource(getResources(), R.drawable.pin_10), 
		};

		// ボールがピンにぶつかったときのエフェクト画像
		effectImage = BitmapFactory.decodeResource(getResources(),
				R.drawable.effect);
		// 結果画像
		resultImage = BitmapFactory.decodeResource(getResources(),
				R.drawable.result);

		// 数字の画像
		numberImages = new Bitmap[11];
		for (int i = 0; i <= 10; i++) {
			numberImages[i] = BitmapFactory.decodeResource(
					getResources(),
					getResources().getIdentifier(String.format("c%02d", i),
							"drawable", getContext().getPackageName()));
		}
		bgRect = new Rect(0, 0, getWidth(), getHeight());
		pinRect = new Rect(0, 0, getWidth(), getHeight() * 340 / 800);
		resultRect = new Rect(0, getHeight() * 214 / 800, getWidth(),
				getHeight() * 566 / 800);
		numberRect = new Rect(getWidth() * 104 / 480, getHeight() * 388 / 800,
				getWidth() * 227 / 480, getHeight() * 520 / 800);

		if (thread == null) {
			thread = new Thread(this);
			runFlag = true;
			thread.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		runFlag = false;
		thread = null;
	}

	/**
	 * 描画
	 */
	public void doDraw() {
		Canvas canvas = getHolder().lockCanvas();

		canvas.drawBitmap(bgImage, null, bgRect, paint);
		canvas.drawBitmap(pinImages[pinIndex], null, pinRect, paint);

		if (state == STATE_ROLL) {
			canvas.drawBitmap(ballImages[ballIndex / 2], x, y, paint);
			ballIndex = (ballIndex + 1) % (ballImages.length * 2);
			y -= speed;
			// 各軌道
			switch (curve) {
			case 0:
				x = getWidth()
						/ 2
						+ getWidth()
						* 0.3f
						* (1.0f - (float) Math.pow(1.5, (getHeight() * 340
								/ 800 - ballImages[0].getHeight() - y) / 50.0));
				break;
			case 1:
				x = getWidth()
						/ 2
						+ getWidth()
						* 0.2f
						* (1.0f - (float) Math.pow(1.2, (getHeight() * 340
								/ 800 - ballImages[0].getHeight() - y) / 50.0));
				break;
			case 2:
				x = getWidth()
						/ 2
						+ getWidth()
						* 0.1f
						* (1.0f - (float) Math.pow(1.2, (getHeight() * 340
								/ 800 - ballImages[0].getHeight() - y) / 50.0));
				break;
			case 3:
				x = getWidth() / 3;
				break;
			case 4:
				x = getWidth()
						/ 2
						- ballImages[0].getWidth()
						/ 2
						+ getWidth()
						/ 3
						* (float) Math.sin((getHeight() * 340 / 800
								- ballImages[0].getHeight() - y) / 100.0);
				break;
			case 5:
				x = y - getHeight() / 2;
				break;
			}
			if (x < 0) {
				x = 0.0f;
			}
			if (y + ballImages[0].getHeight() < getHeight() * 340 / 800) {
				count++;
				// ピンにぶつかったらエフェクト
				if (curve != 5) {
					canvas.drawBitmap(effectImage, null, pinRect, paint);
				}
				if (pinIndex == 0) {
					pinIndex = pinImages.length - curve - 1;
				}

				// ボールが過ぎ去ったら結果画面へ
				if (y + ballImages[0].getHeight() < 0 && count > 40) {
					state = STATE_RESULT;
				}
			}
		} else if (state == STATE_RESULT) {
			// 結果表示
			canvas.drawBitmap(resultImage, null, resultRect, paint);
			canvas.drawBitmap(numberImages[numPins[pinIndex]], null,
					numberRect, paint);
		}

		getHolder().unlockCanvasAndPost(canvas);
	}

	@Override
	public void run() {
		while (runFlag) {
			doDraw();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ゲームの状況を取得
	 * 
	 * @return ゲームの状況
	 */
	public int getState() {
		return state;
	}

	/**
	 * ボールを投げるモーションに入る
	 */
	public void doThrow() {
		if (state == STATE_INIT) {
			state = STATE_THROW;
		}
	}

	/**
	 * 初期化
	 */
	public void doInit() {
		if (state == STATE_RESULT) {
			state = STATE_INIT;
			pinIndex = 0;
		}
	}

	/**
	 * ボールを転がす
	 * 
	 * @param speed
	 *            ボールのスピード(px/frame)
	 * @param curve
	 *            ボールの曲がり具合(m/(s*s))
	 */
	public void doRoll(float speed, float curve) {
		if (state == STATE_THROW) {
			x = getWidth() / 2;
			y = getHeight();
			count = 0;

			// speedの上限、下限
			if (speed < 8.0f) {
				speed = 8.0f;
			}
			if (speed > 30.0f) {
				speed = 30.0f;
			}
			this.speed = speed;

			// 曲がり具合に合わせて軌道を決める
			if (Math.abs(curve) < 0.5f) {
				this.curve = 0;
			} else if (Math.abs(curve) < 1.0f) {
				this.curve = 1;
			} else if (Math.abs(curve) < 1.5f) {
				this.curve = 2;
			} else if (Math.abs(curve) < 2.0f) {
				this.curve = 3;
			} else if (Math.abs(curve) < 2.5f) {
				this.curve = 4;
			} else {
				// ガーター
				this.curve = 5;
			}
			state = STATE_ROLL;
		}
	}
}
