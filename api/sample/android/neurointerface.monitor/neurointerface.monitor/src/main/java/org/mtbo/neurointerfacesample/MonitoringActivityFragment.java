package org.mtbo.neurointerfacesample;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Utils;

import org.mtbo.neurointerfacecore.NeuroController;

import java.util.ArrayList;
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class MonitoringActivityFragment extends Fragment {

	@BindView(R.id.chart)
	LineChart chart;

	@BindView(R.id.log)
	EditText log_text;

	final LinkedList<NeuroController.Measurement> values = new LinkedList<>();

	public MonitoringActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_monitoring, container, false);

		ButterKnife.bind(this, v);
		
		setup_chart();
		return v;
	}

	private void setup_chart() {
		chart.setBackgroundColor(Color.WHITE);

		chart.getDescription().setEnabled(false);

		// if more than 60 entries are displayed in the chart, no values will be
		// drawn
		chart.setMaxVisibleValueCount(60);

		// scaling can now only be done on x- and y-axis separately
		chart.setPinchZoom(false);

		chart.setScaleEnabled(false);
		chart.setDragEnabled(false);

		chart.setDrawGridBackground(false);

		XAxis xAxis = chart.getXAxis();
		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setDrawGridLines(false);
		xAxis.setEnabled(false);

		YAxis leftAxis = chart.getAxisLeft();
	        leftAxis.setEnabled(false);
		leftAxis.setLabelCount(7, false);
		leftAxis.setDrawGridLines(false);
		leftAxis.setDrawAxisLine(false);

		YAxis rightAxis = chart.getAxisRight();
		rightAxis.setEnabled(false);

		chart.getLegend().setEnabled(false);
	}

	private void fill_data() {

		synchronized (values) {

			if (values.isEmpty())
				return;

			chart.resetTracking();

			{
				ArrayList<Entry> yVals1 = new ArrayList<Entry>();


				for (int i = 0; i < values.size(); i++) {

					float val = (float) values.get(i).filtered_result;

					yVals1.add(new Entry(
						i, val
					));
				}

				LineDataSet set1 = new LineDataSet(yVals1, "Data Set");

				set1.setDrawIcons(false);
				set1.setAxisDependency(YAxis.AxisDependency.LEFT);
				set1.setLineWidth(1.75f);
				set1.setDrawCircles(false);
				set1.setColor(Color.BLUE);
				set1.setHighLightColor(Color.RED);
				set1.setDrawValues(false);
				set1.setDrawFilled(true);
				set1.setHighlightEnabled(true);
				set1.setHighLightColor(Color.TRANSPARENT);

				if (Utils.getSDKInt() >= 18) {
					Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.fade_red);
					set1.setFillDrawable(drawable);
				} else {
					set1.setFillColor(getResources().getColor(R.color.red_fill));
				}

				LineData data = new LineData(set1);

				chart.setData(data);
			}


			chart.invalidate();
		}
	}


	public void log(String what) {
		String t = log_text.getText().toString();
		String truncated = t.substring(0, Math.min(1024, t.length()));
		log_text.setText(what + "\n" + truncated);
	}

	public void append_data(NeuroController.Measurement measurement) {
		synchronized (values)
		{
			if (values.size() >= 50)
				values.removeFirst();

			values.addLast(measurement);

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					fill_data();
				}
			});

		}
	}
}
