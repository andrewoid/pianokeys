package pianokeys.net;

public record BitMidiFile(String fileName, byte[] content)
{
    public BitMidiFile {
        content = content.clone();
    }

    @Override
    public byte[] content()
    {
        return content.clone();
    }
}
