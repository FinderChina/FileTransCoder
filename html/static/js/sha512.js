function strULonglongCtxParams() {
	this.uhigh;
	this.ulow;
};

function strSha512sha384CtxParams() {
	this.sz;
	this.counter;
	this.save;
};

function ulonglong_init(uHigh, uLow, isGreater0) {
	var ulonglongCtxParams = new strULonglongCtxParams();
	
	ulonglongCtxParams.uhigh = uHigh & 0xffffffff;
	ulonglongCtxParams.ulow = uLow & 0xffffffff;
	
	return ulonglongCtxParams;
}

function ulonglong_set_hexstr(ulonglongCtxParams, hexStr) {
	var i = 0;
	var j = 0;
	var tmpInt = 0;
	
	ulonglongCtxParams.uhigh = 0;
	ulonglongCtxParams.ulow = 0;
	
	for (i = hexStr.length - 1; i >= 0; i--) {
		tmpInt = hexStr.charCodeAt(i);
		//console.log("tmpInt = %d", tmpInt);
		if (tmpInt >= 48 && tmpInt < 48 + 10) {
			tmpInt -= 48;
		} else if (tmpInt >= 65 && tmpInt < 65 + 6) {
			tmpInt = tmpInt - 65 + 10;
		} else if (tmpInt >= 97 && tmpInt < 97 + 6) {
			tmpInt = tmpInt - 97 + 10;
		} else {
			continue;
		}
		if (j < 8) {
			ulonglongCtxParams.ulow |= (tmpInt << (j * 4));
		} else {
			ulonglongCtxParams.uhigh |= (tmpInt << ((j - 8) * 4));
		}
		j++;
		if (j >= 16) {
			break;
		}
	}
}

function ulonglong_add_equal(ulonglongCtxParamsRe, ulonglongCtxParamsAdd) {
	var tmpLonglongCtxParamsRe = new strULonglongCtxParams();
	
	tmpLonglongCtxParamsRe.uhigh = ulonglongCtxParamsRe.uhigh;
	tmpLonglongCtxParamsRe.ulow = ulonglongCtxParamsRe.ulow;
	
	ulonglong_add(ulonglongCtxParamsRe, tmpLonglongCtxParamsRe, ulonglongCtxParamsAdd);
} 

function ulonglong_u32_add_equal(ulonglongCtxParamsRe, u32Add) {
	var tmpLonglongCtxParamsRe = new strULonglongCtxParams();
	var ulonglongCtxParamsAdd = new strULonglongCtxParams();
	
	tmpLonglongCtxParamsRe.uhigh = ulonglongCtxParamsRe.uhigh;
	tmpLonglongCtxParamsRe.ulow = ulonglongCtxParamsRe.ulow;
	
	ulonglongCtxParamsAdd.uhigh = 0;
	ulonglongCtxParamsAdd.ulow = u32Add;
	
	ulonglong_add(ulonglongCtxParamsRe, tmpLonglongCtxParamsRe, ulonglongCtxParamsAdd);
} 

function ulonglong_add(ulonglongCtxParamsRe, ulonglongCtxParamsAdd1, ulonglongCtxParamsAdd2) {
	var i = 0;
	var ele = 0;
	var carry = 0;
	
	ulonglongCtxParamsRe.ulow = 0;
	for (i = 0; i < 4; i++) {
		ele = ((ulonglongCtxParamsAdd1.ulow >>> (8 * i)) & 0xff) + ((ulonglongCtxParamsAdd2.ulow >>> (8 * i)) & 0xff) + carry;
		if (ele > 255) {
			carry = ele >>> 8;
			ele = ele & 0xff;
		} else {
			carry = 0;
		}
		ulonglongCtxParamsRe.ulow |= (ele << (8 * i));
	}
	
	ulonglongCtxParamsRe.uhigh = 0;
	for (i = 0; i < 4; i++) {
		ele = ((ulonglongCtxParamsAdd1.uhigh >>> (8 * i)) & 0xff) + ((ulonglongCtxParamsAdd2.uhigh >>> (8 * i)) & 0xff) + carry;
		if (ele > 255) {
			carry = ele >> 8;
			ele = ele & 0xff;
		} else {
			carry = 0;
		}
		ulonglongCtxParamsRe.uhigh |= (ele << (8 * i));
	}
}

function ulonglong_minus(ulonglongCtxParamsRe, minuend, subtractor) {
	var i = 0;
	var ele = 0;
	var carry = 0;
	
	ulonglongCtxParamsRe.ulow = 0;
	for (i = 0; i < 4; i++) {
		ele = ((minuend.ulow >>> (8 * i)) & 0xff) - ((subtractor.ulow >>> (8 * i)) & 0xff) - carry;
		if (ele < 0) {
			ele = ele + 256;
			carry = 1;
		} else {
			carry = 0;
		}
		ulonglongCtxParamsRe.ulow |= (ele << (8 * i));
	}
	
	ulonglongCtxParamsRe.uhigh = 0;
	for (i = 0; i < 4; i++) {
		ele = ((minuend.uhigh >>> (8 * i)) & 0xff) - ((subtractor.uhigh >>> (8 * i)) & 0xff) - carry;
		if (ele < 0) {
			ele = ele + 256;
			carry = 1;
		} else {
			carry = 0;
		}
		ulonglongCtxParamsRe.uhigh |= (ele << (8 * i));
	}
	
	if (carry != 0) {
		return false;
	} else {
		return true;
	}
}

function ulonglong_u32add(ulonglongCtxParamsRe, isLow, add1, add2) {
	var i = 0;
	var ele = 0;
	var result = 0;
	var carry = 0;
	
	for (i = 0; i < 4; i++) {
		ele = ((add1 >> (8 * i)) & 0xff) + ((add2 >> (8 * i)) & 0xff) + carry;
		if (ele > 255) {
			carry = ele >> 8;
			ele = ele & 0xff;
		} else {
			carry = 0;
		}
		result |= (ele << (8 * i));
	}
	
	if (isLow) {
		ulonglongCtxParamsRe.ulow = result;
	} else {
		ulonglongCtxParamsRe.uhigh = result;
	}
	
	return carry;
}

function ulonglong_times(ulonglongCtxParamsRe, times1, times2) {
	var i = 0;
	var j = 0;
	var ele = 0;
	var carry = 0;
	
	ulonglongCtxParamsRe.ulow = 0;
	ulonglongCtxParamsRe.uhigh = 0;
	
	carry = 0;
	for (i = 0; i < 4; i++) {		
		carry = 0;
		for (j = 0; j < 4; j++) {			
			ele = ((times1.ulow >>> (8 * j)) & 0xff) * ((times2.ulow >>> (8 * i)) & 0xff) + carry;
			if (ele > 255) {
				carry = ele >> 8;
				ele = ele & 0xff;
			} else {
				carry = 0;
			}
			if (i + j < 4) {
				ulonglongCtxParamsRe.uhigh += ulonglong_u32add(ulonglongCtxParamsRe, true, ulonglongCtxParamsRe.ulow, ele << (8 * (i + j)));
			} else {
				ulonglong_u32add(ulonglongCtxParamsRe, false, ulonglongCtxParamsRe.uhigh, ele << (8 * (i + j - 4)));
			}
		}
		for (j = 0; j < 4; j++) {
			ele = ((times1.uhigh >>> (8 * j)) & 0xff) * ((times2.ulow >>> (8 * i)) & 0xff) + carry;
			if (ele > 255) {
				carry = ele >> 8;
				ele = ele & 0xff;
			} else {
				carry = 0;
			}
			if (i + j < 4) {
				ulonglong_u32add(ulonglongCtxParamsRe, false, ulonglongCtxParamsRe.uhigh, ele << (8 * (i + j)));
			} else {
				//溢出
			}
		}
	}

	for (i = 0; i < 4; i++) {
		carry = 0;
		for (j = 0; j < 4; j++) {
			ele = ((times1.ulow >>> (8 * j)) & 0xff) * ((times2.uhigh >>> (8 * i)) & 0xff) + carry;
			if (ele > 255) {
				carry = ele >> 8;
				ele = ele & 0xff;
			} else {
				carry = 0;
			}
			if (i + j < 4) {
				ulonglongCtxParamsRe.uhigh += (ele << (8 * (i + j)));
			} else {
				//溢出
			}
		}
		for (j = 0; j < 4; j++) {
			ele = ((times1.uhigh >>> (8 * j)) & 0xff) * ((times2.uhigh >>> (8 * i)) & 0xff) + carry;
			if (ele > 255) {
				carry = ele >> 8;
				ele = ele & 0xff;
			} else {
				carry = 0;
			}
			if (i + j < 4) {
				//溢出
			} else {
				//溢出
			}
		}
	}
}


function ulonglong_divide(ulonglongCtxParamsQuotients, ulonglongCtxParamsRemainders, dividend, divider) {
	var i = 0;
	var startIndex = -1;

	ulonglongCtxParamsQuotients.uhigh = 0;
	ulonglongCtxParamsQuotients.ulow = 0;	

	ulonglongCtxParamsRemainders.uhigh = 0;
	ulonglongCtxParamsRemainders.ulow = 0;

	for(startIndex = 0; ; startIndex++) {
		if (ulonglong_u32_compare(ulonglongCtxParamsRemainders, divider) >= 0) {
			if (64 - startIndex > 32) {
				ulonglongCtxParamsQuotients.uhigh |= (0x00000001 << (32 - startIndex));				
			} else {
				ulonglongCtxParamsQuotients.ulow |= (0x00000001 << (64 - startIndex));
			}
			//减法
			if (ulonglong_u64_compare(ulonglongCtxParamsRemainders.ulow, divider.ulow) > 0) {
				ulonglongCtxParamsRemainders.ulow = ulonglongCtxParamsRemainders.ulow - divider.ulow;
			} else {
				ulonglongCtxParamsRemainders.ulow = ~(divider.ulow - ulonglongCtxParamsRemainders.ulow);
				ulonglongCtxParamsRemainders.uhigh--;
			}
			ulonglongCtxParamsRemainders.uhigh = ulonglongCtxParamsRemainders.uhigh - divider.uhigh;
		}
		if (startIndex >= 64) {
			break;
		}
		ulonglongCtxParamsRemainders.uhigh = ulonglongCtxParamsRemainders.uhigh << 1;
		if ((ulonglongCtxParamsRemainders.ulow & 0x80000000) != 0) {
			ulonglongCtxParamsRemainders.uhigh |= 0x00000001;
		}
		ulonglongCtxParamsRemainders.ulow = ulonglongCtxParamsRemainders.ulow << 1;
		if (startIndex < 32) {
			if (dividend.uhigh & (0x80000000 >>> startIndex)) {
				ulonglongCtxParamsRemainders.ulow |= 0x00000001;
			}
		} else {
			if (dividend.ulow & (0x80000000 >>> (startIndex - 32))) {
				ulonglongCtxParamsRemainders.ulow |= 0x00000001;
			}
		}
	}
}


function ulonglong_u32_compare(uint1, uint2) {
	uint1 &= 0xffffffff;
	uint2 &= 0xffffffff;
	
	if ((uint1 & 0x80000000) == 0 && (uint2 & 0x80000000) == 0) {
		return uint1 > uint2 ? 1 : (uint1 == uint2 ? 0 : (-1));
	}
	if ((uint1 & 0x80000000) != 0 && (uint2 & 0x80000000) != 0) {
		uint1 &= 0x7fffffff;
		uint2 &= 0x7fffffff;
		return uint1 > uint2 ? 1 : (uint1 == uint2 ? 0 : (-1));
	}
	if ((uint1 & 0x80000000) != 0) {
		return 1;
	} else {
		return -1;
	}
}

function ulonglong_u64_compare(uint1, uint2) {
	var re = ulonglong_u32_compare(uint1.uhigh, uint2.uhigh);
	if (re > 0) {
		return 1;
	}
	if (re == 0) {
		return ulonglong_u32_compare(uint1.ulow, uint2.ulow);
	} else {
		return -1;
	}
}

function ulonglong_umove(ulonglongCtxParamsRemainders, bits/* > 0 left greater, < 0 right lesser*/) {
	if (bits == 0) {
		return;
	}
	if (bits > 0) {
		if (bits >= 64) {
			ulonglongCtxParamsRemainders.uhigh = 0;
			ulonglongCtxParamsRemainders.ulow = 0;
			return ;
		}
		if (bits >= 32) {
			ulonglongCtxParamsRemainders.uhigh = (ulonglongCtxParamsRemainders.ulow << (bits - 32)) & 0xffffffff;
			ulonglongCtxParamsRemainders.ulow = 0;
			return;
		}
		ulonglongCtxParamsRemainders.uhigh = ((ulonglongCtxParamsRemainders.uhigh << bits) & 0xffffffff) | ((ulonglongCtxParamsRemainders.ulow >>> (32 - bits)) & 0xffffffff);
		ulonglongCtxParamsRemainders.ulow = (ulonglongCtxParamsRemainders.ulow << bits) & 0xffffffff;
		return;
	}
	if (bits < 0) {
		bits = (-bits);
		if (bits >= 64) {
			ulonglongCtxParamsRemainders.uhigh = 0;
			ulonglongCtxParamsRemainders.ulow = 0;
			return ;
		}
		if (bits >= 32) {
			ulonglongCtxParamsRemainders.ulow = ((ulonglongCtxParamsRemainders.uhigh & 0xffffffff) >>> (bits - 32)) & 0xffffffff;
			ulonglongCtxParamsRemainders.uhigh = 0;
			return;
		}
		ulonglongCtxParamsRemainders.ulow = ((ulonglongCtxParamsRemainders.ulow >>> bits) & 0xffffffff) | ((ulonglongCtxParamsRemainders.uhigh << (32 - bits)) & 0xffffffff);
		ulonglongCtxParamsRemainders.uhigh = (ulonglongCtxParamsRemainders.uhigh >>> bits) & 0xffffffff;
	}
}

var constant_512 = [
	"0x428a2f98d728ae22ULL", "0x7137449123ef65cdULL",
	"0xb5c0fbcfec4d3b2fULL", "0xe9b5dba58189dbbcULL",
	"0x3956c25bf348b538ULL", "0x59f111f1b605d019ULL",
	"0x923f82a4af194f9bULL", "0xab1c5ed5da6d8118ULL",
	"0xd807aa98a3030242ULL", "0x12835b0145706fbeULL",
	"0x243185be4ee4b28cULL", "0x550c7dc3d5ffb4e2ULL",
	"0x72be5d74f27b896fULL", "0x80deb1fe3b1696b1ULL",
	"0x9bdc06a725c71235ULL", "0xc19bf174cf692694ULL",
	"0xe49b69c19ef14ad2ULL", "0xefbe4786384f25e3ULL",
	"0x0fc19dc68b8cd5b5ULL", "0x240ca1cc77ac9c65ULL",
	"0x2de92c6f592b0275ULL", "0x4a7484aa6ea6e483ULL",
	"0x5cb0a9dcbd41fbd4ULL", "0x76f988da831153b5ULL",
	"0x983e5152ee66dfabULL", "0xa831c66d2db43210ULL",
	"0xb00327c898fb213fULL", "0xbf597fc7beef0ee4ULL",
	"0xc6e00bf33da88fc2ULL", "0xd5a79147930aa725ULL",
	"0x06ca6351e003826fULL", "0x142929670a0e6e70ULL",
	"0x27b70a8546d22ffcULL", "0x2e1b21385c26c926ULL",
	"0x4d2c6dfc5ac42aedULL", "0x53380d139d95b3dfULL",
	"0x650a73548baf63deULL", "0x766a0abb3c77b2a8ULL",
	"0x81c2c92e47edaee6ULL", "0x92722c851482353bULL",
	"0xa2bfe8a14cf10364ULL", "0xa81a664bbc423001ULL",
	"0xc24b8b70d0f89791ULL", "0xc76c51a30654be30ULL",
	"0xd192e819d6ef5218ULL", "0xd69906245565a910ULL",
	"0xf40e35855771202aULL", "0x106aa07032bbd1b8ULL",
	"0x19a4c116b8d2d0c8ULL", "0x1e376c085141ab53ULL",
	"0x2748774cdf8eeb99ULL", "0x34b0bcb5e19b48a8ULL",
	"0x391c0cb3c5c95a63ULL", "0x4ed8aa4ae3418acbULL",
	"0x5b9cca4f7763e373ULL", "0x682e6ff3d6b2b8a3ULL",
	"0x748f82ee5defb2fcULL", "0x78a5636f43172f60ULL",
	"0x84c87814a1f0ab72ULL", "0x8cc702081a6439ecULL",
	"0x90befffa23631e28ULL", "0xa4506cebde82bde9ULL",
	"0xbef9a3f7b2c67915ULL", "0xc67178f2e372532bULL",
	"0xca273eceea26619cULL", "0xd186b8c721c0c207ULL",
	"0xeada7dd6cde0eb1eULL", "0xf57d4f7fee6ed178ULL",
	"0x06f067aa72176fbaULL", "0x0a637dc5a2c898a6ULL",
	"0x113f9804bef90daeULL", "0x1b710b35131c471bULL",
	"0x28db77f523047d84ULL", "0x32caab7b40c72493ULL",
	"0x3c9ebe0a15c9bebcULL", "0x431d67c49c100d4cULL",
	"0x4cc5d4becb3e42b6ULL", "0x597f299cfc657e2aULL",
	"0x5fcb6fab3ad6faecULL", "0x6c44198c4a475817ULL"
];

function sha512_init() {
	return sha512_sha384_init(false);
}

function sha384_init() {
	return sha512_sha384_init(true);
}

function sha512_sha384_init(isSha384) {
	var sha512sha384CtxParams = new strSha512sha384CtxParams();
	var i = 0;

	sha512sha384CtxParams.sz = new Array(2);
	sha512sha384CtxParams.counter = new Array(8);
	sha512sha384CtxParams.save = new Uint8Array(128);
	
	for (i = 0; i < sha512sha384CtxParams.sz.length; i++) {
		sha512sha384CtxParams.sz[i] = new strULonglongCtxParams();
	}
	for (i = 0; i < sha512sha384CtxParams.counter.length; i++) {
		sha512sha384CtxParams.counter[i] = new strULonglongCtxParams();
	}
	
	sha512sha384CtxParams.sz[0].uhigh = 0;
	sha512sha384CtxParams.sz[0].ulow = 0;
	sha512sha384CtxParams.sz[1].uhigh = 0;
	sha512sha384CtxParams.sz[1].ulow = 0;
	
	if (isSha384 == false) {
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[0], "0x6a09e667f3bcc908ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[1], "0xbb67ae8584caa73bULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[2], "0x3c6ef372fe94f82bULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[3], "0xa54ff53a5f1d36f1ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[4], "0x510e527fade682d1ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[5], "0x9b05688c2b3e6c1fULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[6], "0x1f83d9abfb41bd6bULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[7], "0x5be0cd19137e2179ULL");
	} else {
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[0], "0xCBBB9D5DC1059ED8ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[1], "0x629A292A367CD507ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[2], "0x9159015A3070DD17ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[3], "0x152FECD8F70E5939ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[4], "0x67332667FFC00B31ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[5], "0x8EB44A8768581511ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[6], "0xDB0C2E0D64F98FA7ULL");
		ulonglong_set_hexstr(sha512sha384CtxParams.counter[7], "0x47B5481DBEFA4FA4ULL");
	}

	return sha512sha384CtxParams;
}

function sha512_sha384_update(sha512sha384CtxParams, databytes, byteslen) {
	var len = byteslen;
	var offset = 0;
	var old_sz = new strULonglongCtxParams();
	var l = 0;
	var indataindex = 0;
	var i = 0, j = 0;
	var current = new Array(16);
	
	for (i = 0; i < current.length; i++) {
		current[i] = new strULonglongCtxParams();
	}
	
	old_sz.uhigh = sha512sha384CtxParams.sz[0].uhigh;
	old_sz.ulow = sha512sha384CtxParams.sz[0].ulow;
	
	ulonglong_u32_add_equal(sha512sha384CtxParams.sz[0], len * 8);
	if (ulonglong_u64_compare(sha512sha384CtxParams.sz[0], old_sz) < 0) {
		ulonglong_u32_add_equal(sha512sha384CtxParams.sz[1], 1);
	}
	//offset = (old_sz / 8) % 128;
	ulonglong_umove(old_sz, -3);
	offset = old_sz.ulow & 127;
	
	//console.log("offset = %d", offset);
	
	indataindex = 0;
	while(len > 0){
		l = len < (128 - offset) ? len : (128 - offset);
		for (j = 0; j < l; j++) {
			sha512sha384CtxParams.save[offset + j] = databytes[indataindex + j];
		}
		offset += l;
		indataindex += l;
		len -= l;
		if(offset == 128) {
			for(i = 0; i < 8; i++){
				//current[2 * i + 0] = SwapUint64(*((unsigned long long*)(m_Save + i * 16)));
				//current[2 * i + 1] = SwapUint64(*((unsigned long long*)(m_Save + i * 16 + 8)));
				SwapUint64(current[2 * i + 0], sha512sha384CtxParams.save, i * 16);
				SwapUint64(current[2 * i + 1], sha512sha384CtxParams.save, i * 16 + 8);
			}
			Calc(sha512sha384CtxParams, current);
			offset = 0;
		}
	}
}

function sha512_final(sha512sha384CtxParams) {
	return sha512_sha384_final(sha512sha384CtxParams, false);
}

function sha384_final(sha512sha384CtxParams) {
	return sha512_sha384_final(sha512sha384CtxParams, true);
}

function sha512_sha384_final(sha512sha384CtxParams, isSha384) {
	/*unsigned char zeros[128 + 16];
    unsigned offset = (m_Sz[0] / 8) % 128;
    unsigned int dstart = (240 - offset - 1) % 128 + 1;
	int i = 0;
	unsigned char *r = NULL;*/
	var zeros = new Uint8Array(128 + 16);
	var offset = 0;
	var dstart = 0;
	var i = 0;
	var j = 0;
	var tmpLL = new strULonglongCtxParams();
	var result = null;
	
	if (isSha384) {
		result = new Uint8Array(48);
	} else {
		result = new Uint8Array(64);
	}
	
	tmpLL.uhigh = sha512sha384CtxParams.sz[0].uhigh;
	tmpLL.ulow = sha512sha384CtxParams.sz[0].ulow;
	
	ulonglong_umove(tmpLL, -3);
	offset = tmpLL.ulow & 127;
	
	dstart = (240 - offset - 1) % 128 + 1;
	
	for (i = 0; i < zeros.length; i++) {
		zeros[i] = 0;
	}
    //*zeros = 0x80;
	zeros[0] = 0x80;
    //memset (zeros + 1, 0, sizeof(zeros) - 1);
    /*zeros[dstart+15] = (m_Sz[0] >> 0) & 0xff;
    zeros[dstart+14] = (m_Sz[0] >> 8) & 0xff;
    zeros[dstart+13] = (m_Sz[0] >> 16) & 0xff;
    zeros[dstart+12] = (m_Sz[0] >> 24) & 0xff;
    zeros[dstart+11] = (m_Sz[0] >> 32) & 0xff;
    zeros[dstart+10] = (m_Sz[0] >> 40) & 0xff;
    zeros[dstart+9]  = (m_Sz[0] >> 48) & 0xff;
    zeros[dstart+8]  = (m_Sz[0] >> 56) & 0xff;*/
	for (i = 15; i >= 8; i--) {
		if (15 - i < 4) {
			zeros[dstart+i] = (sha512sha384CtxParams.sz[0].ulow >>> (8 * (15 - i))) & 0xff;
		} else {
			zeros[dstart+i] = (sha512sha384CtxParams.sz[0].uhigh >>> (8 * (11 - i))) & 0xff;
		}
	}
	
	/*
    zeros[dstart+7] = (m_Sz[1] >> 0) & 0xff;
    zeros[dstart+6] = (m_Sz[1] >> 8) & 0xff;
    zeros[dstart+5] = (m_Sz[1] >> 16) & 0xff;
    zeros[dstart+4] = (m_Sz[1] >> 24) & 0xff;
    zeros[dstart+3] = (m_Sz[1] >> 32) & 0xff;
    zeros[dstart+2] = (m_Sz[1] >> 40) & 0xff;
    zeros[dstart+1] = (m_Sz[1] >> 48) & 0xff;
    zeros[dstart+0] = (m_Sz[1] >> 56) & 0xff;*/
	for (i = 7; i >= 0; i--) {
		if (7 - i < 4) {
			zeros[dstart+i] = (sha512sha384CtxParams.sz[1].ulow >>> (8 * (7 - i))) & 0xff;
		} else {
			zeros[dstart+i] = (sha512sha384CtxParams.sz[1].uhigh >>> (8 * (3 - i))) & 0xff;
		}
	}
	
    //Update (zeros, dstart + 16);
	sha512_sha384_update(sha512sha384CtxParams, zeros, dstart + 16);

	//r = (unsigned char*)result;

	for (i = 0; i < 8; ++i) {
		if (isSha384 && i >= 6) {
			break;
		}
	    /*r[8*i+7] = m_Counter[i] & 0xFF;
	    r[8*i+6] = (m_Counter[i] >> 8) & 0xFF;
	    r[8*i+5] = (m_Counter[i] >> 16) & 0xFF;
	    r[8*i+4] = (m_Counter[i] >> 24) & 0xFF;
	    r[8*i+3] = (m_Counter[i] >> 32) & 0XFF;
	    r[8*i+2] = (m_Counter[i] >> 40) & 0xFF;
	    r[8*i+1] = (m_Counter[i] >> 48) & 0xFF;
	    r[8*i]   = (m_Counter[i] >> 56) & 0xFF;
		*/
		for (j = 7; j >= 0; j--) {
			if (7 - j < 4) {
				result[8 * i + j] = (sha512sha384CtxParams.counter[i].ulow >>> (8 * (7 - j))) & 0xff;
			} else {
				result[8 * i + j] = (sha512sha384CtxParams.counter[i].uhigh >>> (8 * (3 - j))) & 0xff;
			}
		}
	} 

	return result;	
}

function ROTR(longlongCtxParams, n) {
	//return (((x)>>>(n)) | ((x) << (64 - (n))));
	var tmpLonglong1 = new strULonglongCtxParams();
	var tmpLonglong2 = new strULonglongCtxParams();
	
	tmpLonglong1.uhigh = longlongCtxParams.uhigh;
	tmpLonglong1.ulow = longlongCtxParams.ulow;
	
	tmpLonglong2.uhigh = longlongCtxParams.uhigh;
	tmpLonglong2.ulow = longlongCtxParams.ulow;
	
	ulonglong_umove(tmpLonglong1, -n);
	ulonglong_umove(tmpLonglong2, 64-n);
	
	tmpLonglong1.uhigh |= tmpLonglong2.uhigh;
	tmpLonglong1.ulow |= tmpLonglong2.ulow;
	
	return tmpLonglong1;
}

function Sigma0Lower(longlongCtxParams) {
	//return (ROTR(x,1)  ^ ROTR(x,8)  ^ ((x)>>>7));	
	var tmpLonglong1 = ROTR(longlongCtxParams, 1);
	var tmpLonglong2 = ROTR(longlongCtxParams, 8);
	var tmpLonglong3 = new strULonglongCtxParams();
	
	tmpLonglong3.uhigh = longlongCtxParams.uhigh;
	tmpLonglong3.ulow = longlongCtxParams.ulow;
	ulonglong_umove(tmpLonglong3, -7);
	
	tmpLonglong1.uhigh = tmpLonglong1.uhigh ^ tmpLonglong2.uhigh ^ tmpLonglong3.uhigh;
	tmpLonglong1.ulow = tmpLonglong1.ulow ^ tmpLonglong2.ulow ^ tmpLonglong3.ulow;
	
	return tmpLonglong1;
}

function Sigma1Lower(longlongCtxParams) {
	//#define sigma1(x) (ROTR(x,19) ^ ROTR(x,61) ^ ((x)>>6))
	//var tmpLonglong1 = ROTR(longlongCtxParams, 19);
	var tmpLonglong2 = ROTR(longlongCtxParams, 61);
	var tmpLonglong1 = ROTR(longlongCtxParams, 19);
	var tmpLonglong3 = new strULonglongCtxParams();

	
	tmpLonglong3.uhigh = longlongCtxParams.uhigh;
	tmpLonglong3.ulow = longlongCtxParams.ulow;
	ulonglong_umove(tmpLonglong3, -6);
	
	tmpLonglong1.uhigh = tmpLonglong1.uhigh ^ tmpLonglong2.uhigh ^ tmpLonglong3.uhigh;
	tmpLonglong1.ulow = tmpLonglong1.ulow ^ tmpLonglong2.ulow ^ tmpLonglong3.ulow;
	
	return tmpLonglong1;
}

function Sigma0Upper(longlongCtxParams) {
	//#define Sigma0(x)	(ROTR(x,28) ^ ROTR(x,34) ^ ROTR(x,39))
	var tmpLonglong1 = ROTR(longlongCtxParams, 28);
	var tmpLonglong2 = ROTR(longlongCtxParams, 34);
	var tmpLonglong3 = ROTR(longlongCtxParams, 39);

	
	tmpLonglong1.uhigh = tmpLonglong1.uhigh ^ tmpLonglong2.uhigh ^ tmpLonglong3.uhigh;
	tmpLonglong1.ulow = tmpLonglong1.ulow ^ tmpLonglong2.ulow ^ tmpLonglong3.ulow;
	
	return tmpLonglong1;
}

function Sigma1Upper(longlongCtxParams) {
	//#define Sigma1(x)	(ROTR(x,14) ^ ROTR(x,18) ^ ROTR(x,41))
	var tmpLonglong1 = ROTR(longlongCtxParams, 14);
	var tmpLonglong2 = ROTR(longlongCtxParams, 18);
	var tmpLonglong3 = ROTR(longlongCtxParams, 41);
	
	tmpLonglong1.uhigh = tmpLonglong1.uhigh ^ tmpLonglong2.uhigh ^ tmpLonglong3.uhigh;
	tmpLonglong1.ulow = tmpLonglong1.ulow ^ tmpLonglong2.ulow ^ tmpLonglong3.ulow;
	
	return tmpLonglong1;
}


function Ch(x, y, z) {
	//#define Ch(x,y,z) (((x) & (y)) ^ ((~(x)) & (z)))
	var tmpLonglong1 = new strULonglongCtxParams();
	
	tmpLonglong1.uhigh = (((x.uhigh) & (y.uhigh)) ^ ((~(x.uhigh)) & (z.uhigh))) & 0xffffffff;
	tmpLonglong1.ulow = (((x.ulow) & (y.ulow)) ^ ((~(x.ulow)) & (z.ulow))) & 0xffffffff;
	
	return tmpLonglong1;
}

function Maj(x, y, z) {
	//#define Maj(x,y,z) (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
	var tmpLonglong1 = new strULonglongCtxParams();
	
	tmpLonglong1.uhigh = (((x.uhigh) & (y.uhigh)) ^ ((x.uhigh) & (z.uhigh)) ^ ((y.uhigh) & (z.uhigh))) & 0xffffffff;
	tmpLonglong1.ulow = (((x.ulow) & (y.ulow)) ^ ((x.ulow) & (z.ulow)) ^ ((y.ulow) & (z.ulow))) & 0xffffffff;
	
	return tmpLonglong1;
}

function Calc(sha512sha384CtxParams, indata)
{
	//unsigned long long AA, BB, CC, DD, EE, FF, GG, HH;
	//unsigned long long data[80];
	//int i;
	var AA = new strULonglongCtxParams();
	var BB = new strULonglongCtxParams();
	var CC = new strULonglongCtxParams();
	var DD = new strULonglongCtxParams();
	var EE = new strULonglongCtxParams();
	var FF = new strULonglongCtxParams();
	var GG = new strULonglongCtxParams();
	var HH = new strULonglongCtxParams();
	var data = new Array(80);
	var i = 0;
	var T1 = new strULonglongCtxParams();
	var T2 = new strULonglongCtxParams();
	var tmpLonglong1 = new strULonglongCtxParams();
	var tmpLonglong2 = new strULonglongCtxParams();
	var tmpLonglong3 = new strULonglongCtxParams();	
	
	for (i = 0; i < data.length; i++) {
		data[i] = new strULonglongCtxParams();
	}

	/*
	AA = m_Counter[0];
	BB = m_Counter[1];
	CC = m_Counter[2];
	DD = m_Counter[3];
	EE = m_Counter[4];
	FF = m_Counter[5];
	GG = m_Counter[6];
	HH = m_Counter[7];
	*/
	AA.uhigh = sha512sha384CtxParams.counter[0].uhigh;
	AA.ulow = sha512sha384CtxParams.counter[0].ulow;
	BB.uhigh = sha512sha384CtxParams.counter[1].uhigh;
	BB.ulow = sha512sha384CtxParams.counter[1].ulow;
	CC.uhigh = sha512sha384CtxParams.counter[2].uhigh;
	CC.ulow = sha512sha384CtxParams.counter[2].ulow;
	DD.uhigh = sha512sha384CtxParams.counter[3].uhigh;
	DD.ulow = sha512sha384CtxParams.counter[3].ulow;
	EE.uhigh = sha512sha384CtxParams.counter[4].uhigh;
	EE.ulow = sha512sha384CtxParams.counter[4].ulow;
	FF.uhigh = sha512sha384CtxParams.counter[5].uhigh;
	FF.ulow = sha512sha384CtxParams.counter[5].ulow;
	GG.uhigh = sha512sha384CtxParams.counter[6].uhigh;
	GG.ulow = sha512sha384CtxParams.counter[6].ulow;
	HH.uhigh = sha512sha384CtxParams.counter[7].uhigh;
	HH.ulow = sha512sha384CtxParams.counter[7].ulow;

	for (i = 0; i < 16; ++i) {
		//data[i] = in[i];
		data[i].uhigh = indata[i].uhigh;
		data[i].ulow = indata[i].ulow;
	}
	for (i = 16; i < 80; ++i) {
		//data[i] = sigma1(data[i-2]) + data[i-7] + sigma0(data[i-15]) + data[i - 16];
		tmpLonglong1 = Sigma1Lower(data[i-2]);
		tmpLonglong2 = Sigma0Lower(data[i-15]);
		ulonglong_add(tmpLonglong3, tmpLonglong1, tmpLonglong2);
		ulonglong_add(tmpLonglong1, data[i-7], data[i - 16]);
		ulonglong_add(data[i], tmpLonglong1, tmpLonglong3);
	}
	//throw Error("322");

	for (i = 0; i < 80; i++) {
		//T1 = HH + Sigma1(EE) + Ch(EE, FF, GG) + constant_512[i] + data[i];
		tmpLonglong1 = Sigma1Upper(EE);
		ulonglong_add(tmpLonglong2, HH, tmpLonglong1);
		tmpLonglong1 = Ch(EE, FF, GG);
		ulonglong_add(tmpLonglong3, tmpLonglong2, tmpLonglong1);
		ulonglong_set_hexstr(tmpLonglong1, constant_512[i]);
		ulonglong_add(tmpLonglong2, tmpLonglong1, data[i]);
		ulonglong_add(T1, tmpLonglong2, tmpLonglong3);
		//console.log("T1 %d %d", T1.uhigh, T1.ulow);	
		
		//T2 = Sigma0(AA) + Maj(AA,BB,CC);
		tmpLonglong1 = Sigma0Upper(AA);	
		tmpLonglong2 = Maj(AA, BB, CC);
		ulonglong_add(T2, tmpLonglong1, tmpLonglong2);
		//console.log("T2 %d %d", T2.uhigh, T2.ulow);	
		//throw Error("T2");

		//HH = GG;
		HH.uhigh = GG.uhigh;
		HH.ulow = GG.ulow;
		
		//GG = FF;
		GG.uhigh = FF.uhigh;
		GG.ulow = FF.ulow;
		
		//FF = EE;
		FF.uhigh = EE.uhigh;
		FF.ulow = EE.ulow;
		
		//EE = DD + T1;
		ulonglong_add(EE, DD, T1);
		
		//DD = CC;
		DD.uhigh = CC.uhigh;
		DD.ulow = CC.ulow;
		
		//CC = BB;
		CC.uhigh = BB.uhigh;
		CC.ulow = BB.ulow;
		
		//BB = AA;
		BB.uhigh = AA.uhigh;
		BB.ulow = AA.ulow;
		
		//AA = T1 + T2;
		ulonglong_add(AA, T1, T2);
	}

	//m_Counter[0] += AA;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[0].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[0].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[0], tmpLonglong1, AA);
	
	//m_Counter[1] += BB;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[1].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[1].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[1], tmpLonglong1, BB);
	
	//m_Counter[2] += CC;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[2].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[2].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[2], tmpLonglong1, CC);
	
	//m_Counter[3] += DD;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[3].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[3].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[3], tmpLonglong1, DD);
	
	//m_Counter[4] += EE;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[4].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[4].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[4], tmpLonglong1, EE);
	
	//m_Counter[5] += FF;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[5].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[5].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[5], tmpLonglong1, FF);
	
	//m_Counter[6] += GG;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[6].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[6].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[6], tmpLonglong1, GG);
	
	//m_Counter[7] += HH;
	tmpLonglong1.uhigh = sha512sha384CtxParams.counter[7].uhigh;
	tmpLonglong1.ulow = sha512sha384CtxParams.counter[7].ulow;
	ulonglong_add(sha512sha384CtxParams.counter[7], tmpLonglong1, HH);
	
}

function SwapUint64(sha512sha384CtxParams, bytesdata, bytesdataindex) {
	var i = 0;
	
	sha512sha384CtxParams.uhigh = 0;
	sha512sha384CtxParams.ulow = 0;
	
	for (i = 0; i < 8; i++) {
		if (i < 4) {
			sha512sha384CtxParams.uhigh |= ((parseInt(bytesdata[i + bytesdataindex]) & 0xff) << ((3 - i) * 8));
		} else {
			sha512sha384CtxParams.ulow |= ((parseInt(bytesdata[i + bytesdataindex]) & 0xff) << ((3 - (i - 4)) * 8));
		}
	}
}

function sha512_sha384_str_to_array(strIn) {
	var bytesArray = new Uint8Array(strIn.length);
	var i = 0;
	
	for (i = 0; i < strIn.length; i++) {
		bytesArray[i] = strIn.charCodeAt(i);
	}
	
	return bytesArray;
}


function sha512_sha384_hex_to_array(hexStrIn) {
  var i = 0;
  var cnt = 0;
  var ele = 0;
  var bytesArray = null;

  cnt = 0;
  for (i = 0; i < hexStrIn.length; i++) {
    ele = hexStrIn.charCodeAt(i);
    if (ele >= 48 && ele < 48 + 10) {
      cnt++;
    }
    if (ele >= 65 && ele < 65 + 6) {
      cnt++;
    }
    if (ele >= 97 && ele < 97 + 6) {
      cnt++;
    }
  }
  bytesArray = new Uint8Array(parseInt((cnt + 1) / 2));
  cnt = 0;
  for (i = 0; i < hexStrIn.length; i++) {
    ele = hexStrIn.charCodeAt(i);
    if (ele >= 48 && ele < 48 + 10) {
      ele -= 48;
      cnt++;
    } else if (ele >= 65 && ele < 65 + 6) {
      ele = ele - 65 + 10;
      cnt++;
    } else if (ele >= 97 && ele < 97 + 6) {
      ele = ele - 97 + 10;
      cnt++;
    } else {
      continue;
    }
    if ((cnt % 2) == 1) {
      bytesArray[parseInt((cnt - 1) / 2)] = (ele << 4) & 0xF0;
    } else {
      bytesArray[parseInt((cnt - 1) / 2)] |= ele;
    }
  }

  return bytesArray;
}

function sha512_sha384_encode_hex(result, len) {
	var hex_digits = "0123456789abcdef";
	var output = new String();
	var i = 0;

	for (i = 0; i < len; i++) {
		output += hex_digits.charAt((result[i] >>> 4) & 0x0f);
		output += hex_digits.charAt((result[i]) & 0x0f);
	}
	
	return output;
}

function localSha512(data){
    var sha512ctx = sha512_init();
    sha512_sha384_update(sha512ctx, sha512_sha384_str_to_array(data), data.length);
    var result1 = sha512_final(sha512ctx);
    return sha512_sha384_encode_hex(result1, 64);
}