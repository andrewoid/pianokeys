
package pianokeys;

import java.io.File;
import java.util.ArrayList;

public class MidiParserTestRun {

    public static void main(String[] args) throws Exception {

        MidiParser parser = new MidiParser();

        ArrayList<Note> notes = parser.parse(
                new File("midi/beethoven-symphony9-4-ode-to-joy-piano-solo.mid")
        );

        System.out.println("Total notes: " + notes.size());

        System.out.println("\nFirst 5 notes:");

        for (int i = 0; i < Math.min(5, notes.size()); i++) {

            Note note = notes.get(i);

            System.out.println(
                    "Pitch: " + note.key()
                            + " | Start: " + note.startTime()
                            + " | Duration: "
                            + (note.endTime() - note.startTime())
                            + " | Instrument: " + note.instrument()
            );
        }
    }
}
