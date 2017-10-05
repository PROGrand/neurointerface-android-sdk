package org.mtbo.neurointerfacesample;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.mtbo.neurointerfacecore.ControllerCallback;
import org.mtbo.neurointerfacecore.DataBroadcaster;
import org.mtbo.neurointerfacecore.NeuroController;
import org.mtbo.neurointerfacecore.NeuroControllerPacket;
import org.mtbo.neurointerfacecore.calibration.CalibrationCallback;
import org.mtbo.neurointerfacecore.calibration.CalibrationFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MonitoringActivity extends AppCompatActivity implements ControllerCallback, CalibrationCallback {

	/// NeuroController - основной класс для работы с нейроинтерфейсом.
	NeuroController controller = new NeuroController();

	@BindView(R.id.toolbar)
	Toolbar toolbar;

	private Thread thread;
	private volatile boolean running = false;

	@OnClick(R.id.discover_button)
	void discover()
	{
		controller.search(MonitoringActivity.this);
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_monitoring);

		ButterKnife.bind(this);

		setSupportActionBar(toolbar);

		/// инициализировать контроллер каллбэком ControllerCallback.
		controller.create(this);

		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try
				{

					while (!thread.isInterrupted()) {

						/// периодически вычитывать текущие данные.
						read_value();
						Thread.sleep(100);
					}
				}
				catch (Exception e)
				{

				}
			}
		});

		thread.start();
	}

	private void read_value() {

		/// Чтение текущих данных.
		final NeuroController.Measurement values = controller.values();

		/// После калибровки NeuroController.Measurement содержит:
		/// filtered_result - уровень активизации/расслабления;
		/// x, y - наклоны головы;
		/// calibrated_value - пороговый уровень, среднее значение между максимальными уровнями активизации и расслабления.

		/// в данном примере уровень активизации/расслабления рисуется в виде графика в отдельном фрагменте.

		for_fragment(new FragmentRunnable() {
			@Override
			void on(MonitoringActivityFragment fragment) {
				fragment.append_data(values);
			}
		});

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/// Необходимый вызов для передачи данных классам-получателям, таким как калибрация.
				DataBroadcaster.broadcast(values);
			}
		});
	}

	@Override
	protected void onDestroy() {
		running = false;
		thread.interrupt();

		try {
			thread.join();
		} catch (InterruptedException e) {

		}

		controller.dispose();
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (controller.onActivityResult(requestCode, resultCode, data))
			return;

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onStart() {
		super.onStart();

		/// Начать приём данных из контроллера.
		controller.start(this);
	}


	@Override
	protected void onStop() {

		/// Остановить приём данных из контроллера.
		controller.stop(this);
		super.onStop();
	}

	/// ControllerCallback

	@Override
	public void found(BluetoothDevice device) {

		/// Обнаружено подходящее Bluetooth устройство.
		log("Controller found, mac: " + device.getAddress());

		/// Соединиться с ним.
		controller.connect(device);
	}


	@Override
	public void enabled() {
		log("Bluetooth enabled");
		discover();
	}

	/// в данном примере лог выводится в отдельном фрагменте.

	private void log(final String what) {
		for_fragment(new FragmentRunnable() {
			@Override
			void on(MonitoringActivityFragment fragment) {
				fragment.log(what);
			}
		});
	}

	@Override
	public void error(@NonNull ErrorCode code) {
		log("Error: " + code.toString());
	}

	@Override
	public void on_packet(final @NonNull NeuroControllerPacket packet) {

		/// Сырые данные с датчиков.
		log("Packet: " + packet.toString());

		/// Если данные не приходят долго (>500 миллисекунд), то считать интерфейс неподключенным.
	}


	@Override
	public void connection_established() {
		log("Connection established");
	}

	@Override
	public void connection_starting() {
		log("Connection starting");
	}

	///////////////////////////////////////


	abstract class FragmentRunnable
	{
		abstract void on(MonitoringActivityFragment fragment);
	}

	private void for_fragment(final FragmentRunnable r)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Fragment f = getSupportFragmentManager().findFragmentById(R.id.monitoring_fragment);

				if (f instanceof MonitoringActivityFragment) {
					MonitoringActivityFragment fragment = MonitoringActivityFragment.class.cast(f);

					r.on(fragment);
				}
			}
		});

	}

	//////////////////////////////////
	/// Калибрация.

	@BindView(R.id.discover_button)
	View discover_button;

	@BindView(R.id.calibration_button)
	View calibration_button;

	private void show_buttons(boolean show) {
		discover_button.setVisibility(show ? View.VISIBLE : View.GONE);
		calibration_button.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@OnClick(R.id.calibration_button)
	void calibrate()
	{
		show_buttons(false);
		CalibrationFragment fragment = new CalibrationFragment();
		getSupportFragmentManager().beginTransaction().addToBackStack("calibration").
			add(R.id.monitoring_fragment, fragment).
			commit();
	}

	//////////////////////////////////////////////
	/// CalibrationCallback

	@Override
	public void calibration_cancel() {
		log("Calibration Canceled");
		getSupportFragmentManager().popBackStack();
		show_buttons(true);
	}

	@Override
	public void calibration_ready() {
		log("Calibration OK");
		getSupportFragmentManager().popBackStack();
		show_buttons(true);
	}

	@Override
	public NeuroController controller() {
		return controller;
	}
}
