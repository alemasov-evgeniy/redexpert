package org.underworldlabs.swing.celleditor;

import com.github.lgooddatepicker.zinternaltools.InternalUtilities;
import org.executequery.gui.resultset.RecordDataItem;
import org.underworldlabs.swing.celleditor.picker.DefaultDateTimezonePicker;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.EventObject;

public class DateTimezoneCellEditor extends AbstractAdjustableCellEditor
        implements TableCellRenderer {

    private final DefaultDateTimezonePicker picker;

    public DateTimezoneCellEditor() {
        picker = new DefaultDateTimezonePicker();
        picker.getDatePicker().getSettings().setGapBeforeButtonPixels(0);
    }

    @Override
    public Object getCellEditorValue() {

        if (picker.getStringValue().isEmpty())
            return null;

        return picker.getOffsetDateTime();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        adjustCellSize(table, column, picker);
        setCellEditorValue(value);

        return picker;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Color color = isSelected ? table.getSelectionBackground() : table.getBackground();
        picker.setBackground(color);
        picker.getTimezonePicker().setBackground(color);
        picker.getDatePicker().setBackground(color);
        picker.getDatePicker().getComponentDateTextField().setBackground(color);

        adjustCellSize(table, column, picker);
        setCellEditorValue(value);
//        adjustCellSize();

        return picker;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return !(e instanceof MouseEvent) || ((MouseEvent) e).getClickCount() >= 1;
    }

    public void setCellEditorValue(Object value) {

        if (value == null) {
            picker.setDateTime((OffsetDateTime) null);
            return;
        }

        if (value instanceof LocalDateTime) {
            picker.setDateTime((LocalDateTime) value);
            return;
        }

        if (value instanceof RecordDataItem) {
            RecordDataItem item = ((RecordDataItem) value);

            if (item.getDisplayValue() instanceof LocalDateTime) {
                picker.setDateTime((LocalDateTime) item.getDisplayValue());

            } else if (item.getDisplayValue() instanceof OffsetDateTime) {
                picker.setDateTime((OffsetDateTime) item.getDisplayValue());

            } else if (item.getDisplayValue() instanceof Timestamp) {
                picker.setDateTime(((Timestamp) item.getDisplayValue()).toLocalDateTime());

            } else if (item.getDisplayValue() instanceof String) {

                String dateTime = (String) item.getDisplayValue();
                String date = dateTime.substring(0, dateTime.indexOf(' '));

                int indexTimezone = dateTime.indexOf('+');
                if (indexTimezone < 0)
                    indexTimezone = dateTime.indexOf('-');

                String time = dateTime.substring(dateTime.indexOf(' ') + 1, indexTimezone);
                LocalDateTime localDateTime = Timestamp.valueOf(date + " " + time).toLocalDateTime();

                picker.setDateTime(localDateTime);

            } else
                picker.setDateTime((OffsetDateTime) null);

        } else {
            String shorterText = InternalUtilities.safeSubstring(value.toString(), 0, 100);
            picker.getDatePicker().setText(shorterText);
        }
    }

}
