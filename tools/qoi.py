import struct, bz2, sys
from PIL import Image
def sx(v,n):
    v&=(1<<n)-1
    return v-(1<<n) if v>>(n-1) else v
def decode_gm_qoi(q):
    assert q[:4]==b'fioq', q[:4]
    w,h,length=struct.unpack_from('<HHI',q,4)
    pos=12; end=12+length
    r=g=b=0;a=255;run=0;index=[0]*256
    out=bytearray(w*h*4); o=0
    for _ in range(w*h):
        if run>0: run-=1
        elif pos<end:
            b1=q[pos];pos+=1
            if b1&0xc0==0x00:
                i=b1<<2; r,g,b,a=index[i:i+4]
            elif b1&0xe0==0x40: run=b1&0x1f
            elif b1&0xe0==0x60:
                b2=q[pos];pos+=1; run=(((b1&0x1f)<<8)|b2)+32
            elif b1&0xc0==0x80:
                r=(r+sx(b1>>4,2))&255; g=(g+sx(b1>>2,2))&255; b=(b+sx(b1,2))&255
            elif b1&0xe0==0xc0:
                b2=q[pos];pos+=1; m=(b1<<8)|b2
                r=(r+sx(m>>8,5))&255; g=(g+sx(m>>4,4))&255; b=(b+sx(m,4))&255
            elif b1&0xf0==0xe0:
                b2,b3=q[pos],q[pos+1];pos+=2; m=(b1<<16)|(b2<<8)|b3
                r=(r+sx(m>>15,5))&255; g=(g+sx(m>>10,5))&255; b=(b+sx(m>>5,5))&255; a=(a+sx(m,5))&255
            else:
                if b1&8: r=q[pos];pos+=1
                if b1&4: g=q[pos];pos+=1
                if b1&2: b=q[pos];pos+=1
                if b1&1: a=q[pos];pos+=1
            i=((r^g^b^a)&63)<<2; index[i:i+4]=[r,g,b,a]
        out[o:o+4]=bytes((r,g,b,a)); o+=4
    return Image.frombytes('RGBA',(w,h),bytes(out))
def decode_blob(blob):
    if blob[:4]==b'\x89PNG':
        import io; return Image.open(io.BytesIO(blob))
    if blob[:4]==b'2zoq':
        return decode_gm_qoi(bz2.decompress(blob[12:]))
    if blob[:4]==b'fioq': return decode_gm_qoi(blob)
    raise ValueError(blob[:8])
